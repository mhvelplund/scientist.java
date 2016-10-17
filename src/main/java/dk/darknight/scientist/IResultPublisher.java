package dk.darknight.scientist;

/** Provides an interface for publishing experiment results. */
public interface IResultPublisher {
	/**
	 * Publishes the results of an experiment.
	 * 
	 * @param <T>
	 *           The type of result being published from an experiment's
	 *           behavior.
	 * @param <TClean>
	 *           The cleaned version of the type optimized for publishing.
	 * @param result
	 *           The result of the experiment.
	 */
	<T, TClean> void publish(Result<T, TClean> result);
}