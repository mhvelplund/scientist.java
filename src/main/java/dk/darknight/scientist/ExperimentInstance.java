package dk.darknight.scientist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;

import dk.darknight.scientist.functions.Action;
import dk.darknight.scientist.functions.DoubleAction;
import dk.darknight.scientist.functions.DoubleFunction;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;

/**
 * An instance of an experiment. This actually runs the control and the
 * candidate and measures the result.
 *
 * @param <T>
 *           The return type of the experiment
 * @param <TClean>
 *           The cleaned type of the experiment
 */
@Getter
@ToString
@EqualsAndHashCode
final class ExperimentInstance<T, TClean> {
	@Value
	private static class NamedBehavior<T> {
		/** Gets the behavior to execute during an experiment. */
		String name;
		
		/** Gets the name of the behavior. */
		Supplier<T> behavior;
	}

	private final static String CONTROL_EXPERIMENT_NAME = "control";

	private static final long CANDIDATE_TIMEOUT_MS = 5000;

	private final Action<Void> beforeRun;
	private final Map<String, Supplier<T>> candidates;
	private final Function<T, ?> cleaner;
	private final Comparator<T> comparator;
	private final int concurrentTasks;
	private final Map<String, Object> contexts = new HashMap<>();
	private final Supplier<T> control;
	private final Supplier<Boolean> enabled;
	private final List<DoubleFunction<T, T, Boolean>> ignores = new ArrayList<>();
	private final String name;
	private final Supplier<Boolean> runIf;
	private final DoubleAction<Operation, Exception> thrown;
	private final boolean throwOnMismatches;
	private final List<NamedBehavior<T>> behaviors = new ArrayList<>();

	public ExperimentInstance(ExperimentSettings<T, TClean> settings) {
		name = settings.getName();
		candidates = settings.getCandidates();
		beforeRun = settings.getBeforeRun();
		cleaner = settings.getCleaner();
		comparator = settings.getComparator();
		concurrentTasks = settings.getConcurrentTasks();
		control = settings.getControl();
		enabled = settings.getEnabled();
		runIf = settings.getRunIf();
		thrown = settings.getThrown();
		throwOnMismatches = settings.isThrowOnMismatches();

		behaviors.add(new NamedBehavior<>(CONTROL_EXPERIMENT_NAME, settings.getControl()));

		Set<Entry<String, Supplier<T>>> entrySet = candidates.entrySet();
		for (Entry<String, Supplier<T>> entry : entrySet) {
			behaviors.add(new NamedBehavior<>(entry.getKey(), entry.getValue()));
		}
	}

	/** Determine whether or not the experiment should run. */
	private boolean shouldExperimentRun() {
		try {
			// Only let the experiment run if at least one candidate (> 1
			// behaviors) is included. The control is always included behaviors
			// count.
			return behaviors.size() > 1 && enabled.get() && runIfAllows();
		} catch (Exception e) {
			thrown.apply(Operation.ENABLED, e);
			return false;
		}
	}

	
	/** Does {@link #runIf} allow the experiment to run? */
	private boolean runIfAllows() {
		try {
			return runIf.get();
		} catch (Exception e) {
			thrown.apply(Operation.RUN_IF, e);
			return false;
		}
	}

	public T run() {
		// Determine if experiments should be run.
		if (!shouldExperimentRun()) {
			return behaviors.get(0).getBehavior().get();
		}

		if (beforeRun != null) {
			beforeRun.apply(null);
		}

		// Randomize ordering...
		Collections.shuffle(behaviors);

		// Break tasks into batches of "ConcurrentTasks" size
		final List<Future<Observation<T, TClean>>> observations = new ArrayList<>();
		final List<String> observationNames = new ArrayList<>();
		Future<Observation<T, TClean>> controlFuture = null;
		final ExecutorService xs = Executors.newFixedThreadPool(concurrentTasks);

		for (NamedBehavior<T> b : behaviors) {
			@SuppressWarnings("unchecked")
			Future<Observation<T, TClean>> o2 = (Future<Observation<T, TClean>>) ((Future<?>) Observation.of(xs,
					b.getName(), b.getBehavior(), thrown, cleaner));
			observations.add(o2);
			observationNames.add(b.getName());

			if (CONTROL_EXPERIMENT_NAME.equals(b.getName())) {
				controlFuture = o2;
			}
		}

		xs.shutdown();

		final Observation<T, TClean> controlObservation;
		try {
			controlObservation = controlFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			throw Throwables.propagate(e);
		}

		Future<Result<T, TClean>> result = publishAsynchronously(observations, observationNames, xs, controlObservation, this);

		if (throwOnMismatches) {
			Result<T, TClean> r;
			try {
				r = result.get();
			} catch (InterruptedException | ExecutionException e) {
				throw Throwables.propagate(e);
			}
			if (r.isMismatched()) {
				throw new MismatchException(name, r);
			}
		}

		if (controlObservation.isThrown()) {
			throw Throwables.propagate(controlObservation.getException());
		}

		return controlObservation.getValue();
	}

	/**
	 * Publish the results asynchronously.
	 * 
	 * @param observations
	 * @param observationNames
	 * @param xs
	 * @param controlObservation
	 * @param instance
	 * @return
	 */
	private Future<Result<T, TClean>> publishAsynchronously(final List<Future<Observation<T, TClean>>> observations,
			final List<String> observationNames, final ExecutorService xs, final Observation<T, TClean> controlObservation,
			final ExperimentInstance<T, TClean> instance) {
		Future<Result<T, TClean>> result = Executors.newSingleThreadScheduledExecutor()
				.submit(new Callable<Result<T, TClean>>() {
					@Override
					public Result<T, TClean> call() throws Exception {
						Result<T, TClean> result = null;
						try {
							List<Observation<T, TClean>> os = resolveObservationFutures(observations, observationNames, xs);
							result = new Result<T, TClean>(instance, os, controlObservation, contexts);
							Scientist.getResultPublisher().publish(result);
						} catch (Exception e) {
							thrown.apply(Operation.PUBLISH, e);
						}
						return result;
					}
				});
		return result;
	}

	private List<Observation<T, TClean>> resolveObservationFutures(
			final List<Future<Observation<T, TClean>>> observations, final List<String> observationNames,
			final ExecutorService xs) throws InterruptedException, ExecutionException {
		if (xs.awaitTermination(CANDIDATE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
			xs.shutdownNow();
		}
		List<Observation<T, TClean>> os = new ArrayList<>();
		for (int i = 0; i < observations.size(); i++) {
			Future<Observation<T, TClean>> f = observations.get(i);
			Observation<T, TClean> o;
			if (f.isDone()) {
				o = f.get();
			} else {
				o = Observation.timedOut(observationNames.get(i));
			}
			os.add(o);
		}
		return os;
	}

	public boolean ignoreMismatchedObservation(Observation<T, TClean> control, Observation<T, TClean> candidate) {
		if (ignores.isEmpty()) {
			return false;
		}

		try {
			List<DoubleFunction<?, ?, ?>> results = new ArrayList<>();
			for (DoubleFunction<T, T, Boolean> i : ignores) {
				if (i.apply(control.getValue(), candidate.getValue())) {
					results.add(i);
				}
			}
			return !results.isEmpty();
		} catch (Exception e) {
			thrown.apply(Operation.IGNORE, e);
			return false;
		}
	}

}