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
	private String name;
	private DoubleAction<Operation, Exception> experimentThrown;
	private Function<T, TClean> cleaner;
	@NonFinal
	private Exception exception;
	@NonFinal
	private long duration;
	@NonFinal
	private T value;

	/** Gets whether an exception was observed. */
	public boolean isThrown() {
		return exception != null;
	}

	public static <T, TClean> Observation<T, TClean> of(String name, Supplier<T> block,
			DoubleAction<Operation, Exception> thrown, Function<T, TClean> cleaner) {
		Observation<T, TClean> observation = new Observation<T, TClean>(name, thrown, cleaner);
		observation.run(block);
		return observation;
	}
	
	public static <T, TClean> Observation<T, TClean> timedOut(String name) {
		Observation<T, TClean> observation = new Observation<T, TClean>(name, null, null);
		observation.exception = new TimeoutException("Experiment failed to complete in time");
		return observation;
	}

	public static <T, TClean> Future<Observation<T, TClean>> of(ExecutorService xs, final String name,
			final Supplier<T> block, final DoubleAction<Operation, Exception> thrown,
			final Function<T, TClean> cleaner) {
		return xs.submit(new Callable<Observation<T, TClean>>() {
			@Override
			public Observation<T, TClean> call() throws Exception {
				return of(name, block, thrown, cleaner);
			}
		});
	}

	private void run(Supplier<T> block) {
		long start = System.currentTimeMillis();
		try {
			value = block.get();
		} catch (Exception ex) {
			exception = ex;
		}

		duration = System.currentTimeMillis() - start;
	}

	public boolean equivalentTo(Observation<T, TClean> other, Comparator<T> comparator) {
		try {
			boolean valuesAreEqual = false;
			boolean bothRaised = other.isThrown() && isThrown();
			boolean neitherRaised = !other.isThrown() && !isThrown();

			if (neitherRaised) {
				// TODO if block_given?
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

}