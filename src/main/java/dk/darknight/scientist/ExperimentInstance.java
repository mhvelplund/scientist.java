package dk.darknight.scientist;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;

import dk.darknight.scientist.functions.Action;
import dk.darknight.scientist.functions.DoubleAction;
import dk.darknight.scientist.functions.DoubleFunction;
import lombok.*;

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
		// TODO: wrap in synchronized ?
		Collections.shuffle(behaviors);
		
//        // Break tasks into batches of "ConcurrentTasks" size
//        var observations = new List<Observation<T, TClean>>();
//        foreach (var behaviors in orderedBehaviors.Chunk(ConcurrentTasks))
//        {
//            // Run batch of behaviors simultaneously
//            var tasks = behaviors.Select(b =>
//            {
//                return Observation<T, TClean>.New(
//                    b.Name,
//                    b.Behavior,
//                    Comparator,
//                    Thrown,
//                    Cleaner);
//            });
//
//            // Collect the observations
//            observations.AddRange(await Task.WhenAll(tasks));
//        }         
		List<Observation<T, TClean>> observations = new ArrayList<>();

		// ... don't break tasks into batches of "ConcurrentTasks" size ... just
		// run then in sequence :)
		Observation<T, TClean> controlObservation = null;
		for (NamedBehavior<T> b : behaviors) {
			@SuppressWarnings("unchecked")
			Observation<T, TClean> o = (Observation<T, TClean>) Observation.of(b.getName(), b.getBehavior(), comparator, thrown, cleaner);
			observations.add(o);
			if (CONTROL_EXPERIMENT_NAME.equals(o.getName()))
				controlObservation = o;
		}
		
		Result<T, TClean> result = new Result<T, TClean>(this, observations, controlObservation, contexts);

//        try
//        {
//            // TODO: Make this Fire and forget so we don't have to wait for this
//            // to complete before we return a result
//            await Scientist.ResultPublisher.Publish(result);
//        }
//        catch (Exception ex)
//        {
//            Thrown(Operation.Publish, ex);
//        }

		if (throwOnMismatches && result.isMismatched()) {
			throw new MismatchException(name, result);
		}
		
		if (controlObservation.isThrown()) {
			throw Throwables.propagate(controlObservation.getException());
		}

		return controlObservation.getValue();
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