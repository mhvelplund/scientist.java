package dk.darknight.scientist;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

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

@Getter
@ToString
@EqualsAndHashCode
final class ExperimentInstance<T, TClean> {
	@Value
	private static class NamedBehavior<T> {
		String name;
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

	boolean shouldExperimentRun() {
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

		List<Observation<T, TClean>> observations = new ArrayList<>();

		// ... don't break tasks into batches of "ConcurrentTasks" size ... just
		// run then in sequence :)
		Observation<T, TClean> controlObservation = null;
		for (NamedBehavior<T> b : behaviors) {
			@SuppressWarnings("unchecked")
			Observation<T, TClean> o = (Observation<T, TClean>) Observation.of(b.getName(), b.getBehavior(), thrown,
					cleaner);
			observations.add(o);
			if (CONTROL_EXPERIMENT_NAME.equals(o.getName()))
				controlObservation = o;
		}

		Result<T, TClean> result = new Result<T, TClean>(this, observations, controlObservation, contexts);

		try {
			Scientist.getResultPublisher().publish(result);
		} catch (Exception e) {
			thrown.apply(Operation.PUBLISH, e);
		}

		if (throwOnMismatches && result.isMismatched()) {
			throw new MismatchException(name, result);
		}

		if (controlObservation.isThrown()) {
			throw Throwables.propagate(controlObservation.getException());
		}

		return controlObservation.getValue();
	}

	public T runParallel() {
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
		final ExecutorService xs = Executors.newWorkStealingPool(concurrentTasks);

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

		final ExperimentInstance<T, TClean> instance = this;

		// Publish the results asynchronously
		Future<Result<T, TClean>> result = Executors.newSingleThreadScheduledExecutor()
				.submit(new Callable<Result<T, TClean>>() {
					@Override
					public Result<T, TClean> call() throws Exception {
						Result<T, TClean> result = null;
						try {
							List<Observation<T, TClean>> os = resolveObservationFutures(observations, observationNames,
									xs);
							result = new Result<T, TClean>(instance, os, controlObservation, contexts);
							try {
								Scientist.getResultPublisher().publish(result);
							} catch (Exception e) {
								thrown.apply(Operation.PUBLISH, e);
							}
						} catch (InterruptedException | ExecutionException e) {
							thrown.apply(Operation.PUBLISH, e);
						}
						return result;
					}
				});

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