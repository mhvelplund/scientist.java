package dk.darknight.scientist;

import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;

import dk.darknight.scientist.functions.DoubleAction;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@RequiredArgsConstructor
class Observation<T, TClean> {
	/**
	 * Create a lazy observation.
	 * 
	 * @param xs an executor service used to execute the observation. The observation is submitted as a {@link Callable}.
	 * @param name the name of the candidate
	 * @param block the actual experiment
	 * @param thrown 
	 * @param cleaner
	 * @return a future observation
	 */
	public static <T, TClean> Future<Observation<T, TClean>> of(ExecutorService xs, final String name,
			final Supplier<T> block, final DoubleAction<Operation, Exception> thrown, final Function<T, TClean> cleaner) {
		return xs.submit(new Callable<Observation<T, TClean>>() {
			@Override
			public Observation<T, TClean> call() throws Exception {
				return of(name, block, thrown, cleaner);
			}
		});
	}

	/**
	 * Create and evaluate an observation.
	 * 
	 * @param name
	 * @param block
	 * @param thrown
	 * @param cleaner
	 * @return
	 */
	public static <T, TClean> Observation<T, TClean> of(String name, Supplier<T> block,
			DoubleAction<Operation, Exception> thrown, Function<T, TClean> cleaner) {
		Observation<T, TClean> observation = new Observation<T, TClean>(name, thrown, cleaner);
		observation.run(block);
		return observation;
	}

	/** Create a dummy observation used for asynchronous publishin to indicate a timed out observation. */
	public static <T, TClean> Observation<T, TClean> timedOut(String name) {
		Observation<T, TClean> observation = new Observation<T, TClean>(name, null, null);
		observation.exception = new TimeoutException("Experiment failed to complete in time");
		return observation;
	}

	/** The name of the experiment candidate. */	
	private String name;
	
	private DoubleAction<Operation, Exception> experimentThrown;
	
	/** A function used to transform results to a more publisher friendly format. Defaults to the identity function. */
	private Function<T, TClean> cleaner;

	@NonFinal
	private Exception exception;

	@NonFinal
	private long duration;

	@NonFinal
	private T value;

	/**
	 * Determine if two observations are equivalent (not necessarily identical).
	 * 
	 * @param other
	 *           another observation to compare with this one
	 * @param comparator
	 *           the comparator to use. A zero return value means the
	 *           observations are equivalent
	 * @return <code>true</code>, if the observations are equivalent
	 */
	public boolean equivalentTo(Observation<T, TClean> other, Comparator<T> comparator) {
		try {
			boolean valuesAreEqual = false;
			boolean bothRaised = other.isThrown() && isThrown();
			boolean neitherRaised = !other.isThrown() && !isThrown();

			if (neitherRaised) {
				valuesAreEqual = comparator.compare(other.value, value) == 0;
			}

			boolean exceptionsAreEquivalent = bothRaised && other.exception.getClass().equals(exception.getClass())
					&& MoreObjects.firstNonNull(other.exception.getMessage(), "")
							.equals(MoreObjects.firstNonNull(exception.getMessage(), ""));

			return (neitherRaised && valuesAreEqual) || (bothRaised && exceptionsAreEquivalent);
		} catch (Exception e) {
			experimentThrown.apply(Operation.COMPARE, e);
			return false;
		}
	}

	/** Gets whether an exception was observed. */
	public boolean isThrown() {
		return exception != null;
	}

	/** Execute a timed experiment and populate the observation. */
	private void run(Supplier<T> block) {
		long start = System.currentTimeMillis();
		try {
			value = block.get();
		} catch (Exception ex) {
			exception = ex;
		}

		duration = System.currentTimeMillis() - start;
	}
		
	public TClean getCleanedValue() {
		return cleaner.apply(value);
	}
}