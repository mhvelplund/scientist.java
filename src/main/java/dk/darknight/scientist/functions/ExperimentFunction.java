package dk.darknight.scientist.functions;

import dk.darknight.scientist.IExperiment;
import dk.darknight.scientist.Scientist;

/**
 * Consumer that consumes an {@link IExperiment} instance.
 * <p>
 * A helper for {@link Scientist#science(String, ExperimentFunction)} that
 * packages the experiemnt in a nicer form.
 * </p>
 * 
 * @param <T>
 *            the return type for the experiment.
 */
abstract public class ExperimentFunction<T> implements Action<IExperiment<T>> {

	@Override
	public final Void apply(IExperiment<T> input) {
		run(input);
		return null;
	}

	abstract public void run(IExperiment<T> input);
}