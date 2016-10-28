package dk.darknight.scientist.functions;

import dk.darknight.scientist.IExperiment;

/**
 * Consumer that consumes an {@link IExperiment} instance.
 * 
 * @param <T>
 *            the return type for the experiment.
 */
public interface ExperimentFunction<T, TClean> {
	 public void apply(IExperiment<T, TClean> input);
}