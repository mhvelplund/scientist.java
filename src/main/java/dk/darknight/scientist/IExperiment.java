package dk.darknight.scientist;

import java.util.Comparator;

import com.google.common.base.Function;
import com.google.common.base.Supplier;

import dk.darknight.scientist.functions.Action;
import dk.darknight.scientist.functions.DoubleAction;
import dk.darknight.scientist.functions.DoubleFunction;

/**
 * Provides an interface for defining a synchronous experiment.
 * <p>
 * <em>The {@code attempt}-method is equal to {@code try} in the original
 * Scientist implementation. It was renamed to avoid a conflict with the
 * reserved keyword.</em>
 * </p>
 * 
 * @param <T>
 *            The return result for the experiment.
 */
public interface IExperiment<T, TClean> {
	/**
	 * Defines data to publish with results.
	 * 
	 * @param key
	 *            The name of the context
	 * @param data
	 *            The context data
	 */
	void addContext(String key, Object data);

	/**
	 * Defines the operation to try.
	 * 
	 * @param name
	 * @param candidate
	 *            The delegate to execute.
	 */
	void attempt(String name, Supplier<T> candidate);

	/**
	 * Defines the operation to try.
	 * 
	 * @param candidate
	 *            The delegate to execute.
	 */
	void attempt(Supplier<T> candidate);

	/**
	 * Define any expensive setup here before the experiment is run.
	 * 
	 * @param action
	 */
	void beforeRun(Action<Void> action);

	/**
	 * Defines a custom func used to compare results.
	 * 
	 * @param comparator
	 */
	void compare(Comparator<T> comparator);

	/**
	 * Defines the check to run to determine if mismatches should be ignored.
	 * 
	 * @param block
	 *            The delegate to execute.
	 */
	void ignore(DoubleFunction<T, T, Boolean> block);

	/** Whether to throw when the control and candidate mismatch. */
	boolean isThrowOnMismatches();

	/**
	 * Defines the check to run to determine if the experiment should run.
	 * 
	 * @param check
	 *            The delegate to evaluate.
	 */
	void runIf(Supplier<Boolean> check);

	/**
	 * Set this flag to throw on experiment mismatches.
	 * <p>
	 * This causes all science mismatches to throw a {@link MismatchException}.
	 * This is intended for test environments and should not be enabled in a
	 * production environment.
	 * </p>
	 * <p>
	 * <em>Note that this forces synchronous reporting, meaning that the
	 * experiment will block until all observations are complete.</em>
	 * </p>
	 * 
	 * @param throwOnMismatches
	 *            Whether to throw when the control and candidate mismatch.
	 */
	void setThrowOnMismatches(boolean throwOnMismatches);

	/**
	 * Defines the exception handler when an exception is thrown during an
	 * experiment.
	 * 
	 * @param block
	 *            The delegate to handle exceptions thrown from an experiment.
	 */
	void thrown(DoubleAction<Operation, Exception> block);

	/**
	 * Defines the operation to actually use.
	 * 
	 * @param control
	 *            The delegate to execute.
	 */
	void use(Supplier<T> control);

	/**
	 * Provides an interface for defining a synchronous experiment that provides
	 * a clean value to publish.
	 * 
	 * @param cleaner
	 *            a method that provides a clean value to publish.
	 */
	void clean(Function<T, TClean> cleaner);
}