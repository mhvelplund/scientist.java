package dk.darknight.scientist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.Value;

@Value
public class Result<T, TClean> {
	/** Gets all of the candidate observations. */
	ImmutableList<Observation<T, TClean>> candidates;

	/** Gets the controlled observation. */
	Observation<T, TClean> control;

	/** Gets the name of the experiment. */
	String experimentName;

	/**
	 * Gets whether the candidate observations matched the controlled
	 * observation.
	 */
	public boolean isMatched() {
		return mismatchedObservations.isEmpty() || !ignoredObservations.isEmpty();
	}

	/**
	 * Gets whether any of the candidate observations did not match the
	 * controlled observation.
	 */
	public boolean isMismatched() {
		return !isMatched();
	};

	/**
	 * Gets all of the observations that did not match the controlled
	 * observation.
	 */
	ImmutableList<Observation<T, TClean>> mismatchedObservations;

	/** Gets all of the observations. */
	ImmutableList<Observation<T, TClean>> observations;

	/** Gets all of the mismatched observations whos values where ignored. */
	ImmutableList<Observation<T, TClean>> ignoredObservations;

	/** Gets the context data supplied to the experiment. */
	public ImmutableMap<String, Object> contexts;

	public Result(ExperimentInstance<T, TClean> experiment, List<Observation<T, TClean>> observations,
			Observation<T, TClean> control, Map<String, Object> contexts) {
		List<Observation<T, TClean>> tmpCandidates = new ArrayList<>();
		tmpCandidates.addAll(observations);
		tmpCandidates.remove(control);
		this.candidates = ImmutableList.copyOf(tmpCandidates);
		this.control = control;
		this.experimentName = experiment.getName();
		this.observations = ImmutableList.copyOf(observations);
		this.contexts = ImmutableMap.copyOf(contexts);

		List<Observation<T, TClean>> tmpMismatchedObservations = new ArrayList<>();
		List<Observation<T, TClean>> tmpIgnoredObservations = new ArrayList<>();

		for (Observation<T, TClean> candidate : candidates) {
			if (!candidate.equivalentTo(control, experiment.getComparator())) {
				if (experiment.ignoreMismatchedObservation(control, candidate)) {
					tmpIgnoredObservations.add(candidate);
				} else {
					tmpMismatchedObservations.add(candidate);
				}
			}
		}
		this.ignoredObservations = ImmutableList.copyOf(tmpIgnoredObservations);
		this.mismatchedObservations = ImmutableList.copyOf(tmpMismatchedObservations);
	}
}