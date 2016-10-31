package dk.darknight.scientist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import dk.darknight.scientist.functions.Action;
import dk.darknight.scientist.functions.DoubleAction;
import dk.darknight.scientist.functions.DoubleFunction;
import dk.darknight.scientist.functions.ExperimentFunction;
import dk.darknight.scientist.util.FractionSummer;

public class ScientistTest {
	/**
	 * Experiment that calculates the sum of a set of fractions, using a rounding
	 * and a non-rounding sum-method.
	 */
	private final class CompareFloatAndIntSummedFractions implements ExperimentFunction<Float, Float> {
		private final FractionSummer fractionSummer;

		private CompareFloatAndIntSummedFractions(FractionSummer fractionSummer) {
			this.fractionSummer = fractionSummer;
		}

		@Override
		public void apply(IExperiment<Float, Float> experiment) {
			experiment.use(floatSumSupplier(fractionSummer));
			experiment.attempt("intSummer", intSumSupplier(fractionSummer));
		}
	}

	private static final int INTEGER_FRACTION_SUM = 7;

	private Supplier<Float> floatSumSupplier(final FractionSummer fractionSummer) {
		return new Supplier<Float>() {
			@Override
			public Float get() {
				return fractionSummer.getFloatSum();
			}
		};
	}

	/** Return a {@link FractionSummer} that will only work with float summing. */
	private FractionSummer getFloatFractionSum() {
		int[] numerators = { 1, 1, 1 };
		int[] denominators = { 3, 3, 3 };
		return new FractionSummer(numerators, denominators);
	}

	/**
	 * Return a {@link FractionSummer} that will give the same result with either
	 * fraction to number method.
	 * <p>
	 * The sum is {@value #INTEGER_FRACTION_SUM}.
	 * </p>
	 */
	private FractionSummer getIntegerFractionSum() {
		int[] numerators = { 2, 4, 8 };
		int[] denominators = { 2, 2, 2 };
		return new FractionSummer(numerators, denominators);
	}

	private Supplier<Float> intSumSupplier(final FractionSummer fractionSummer) {
		return new Supplier<Float>() {
			@Override
			public Float get() {
				return (float) fractionSummer.getIntSum();
			}
		};
	}

	@Before
	public void setup() {
		Scientist.setResultPublisher(LogPublisher.DEFAULT);
		Scientist.setEnabled(Suppliers.ofInstance(true));
	}

	@Test
	public void testBeforeRun() {
		@SuppressWarnings("unchecked")
		final Action<Void> beforeRun = mock(Action.class);
		final FractionSummer fractionSummer = getIntegerFractionSum();

		Scientist.science("beforeRun experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(floatSumSupplier(fractionSummer));
				experiment.attempt("intSummer", intSumSupplier(fractionSummer));
				experiment.beforeRun(beforeRun);
			}
		});

		verify(beforeRun).apply(any(Void.class));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testBeforeRunException() {
		final Action<Void> beforeRun = new Action<Void>() {
			@Override
			public Void apply(Void input) {
				throw new UnsupportedOperationException();
			}
		};

		final FractionSummer fractionSummer = getIntegerFractionSum();

		Scientist.science("beforeRun experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(floatSumSupplier(fractionSummer));
				experiment.attempt("intSummer", intSumSupplier(fractionSummer));
				experiment.beforeRun(beforeRun);
			}
		});
	}

	@Test
	public void testHandledExceptionInEnabledCheck() {
		// Setup
		@SuppressWarnings("unchecked")
		final DoubleAction<Operation, Exception> exceptionHandler = mock(DoubleAction.class);

		// Execute
		final FractionSummer fractionSummer = getIntegerFractionSum();

		Scientist.setEnabled(throwsUnsupportedOperationExceptions());
		Scientist.science("enabled-exception experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(floatSumSupplier(fractionSummer));
				experiment.attempt("intSummer", intSumSupplier(fractionSummer));
				experiment.thrown(exceptionHandler);
			}
		});
		
		// Verify
		verify(exceptionHandler).apply(eq(Operation.ENABLED), any(UnsupportedOperationException.class));
	}

	@Test
	public void testScienceStringExperimentFunctionOfTTClean() {
		FractionSummer fractionSummer = getIntegerFractionSum();

		float sum = Scientist.science("singlethreaded experiment", new CompareFloatAndIntSummedFractions(fractionSummer));

		assertEquals(INTEGER_FRACTION_SUM, sum, 0);
	}

	@Test
	public void testScienceStringIntExperimentFunctionOfTTClean() {
		FractionSummer fractionSummer = spy(getIntegerFractionSum());

		float sum = Scientist.science("multithreaded experiment", 2,
				new CompareFloatAndIntSummedFractions(fractionSummer));

		assertEquals(INTEGER_FRACTION_SUM, sum, 0);
		verify(fractionSummer).getFloatSum();
		verify(fractionSummer).getIntSum();
	}

	@Test
	public void testSetEnabled() {
		FractionSummer fractionSummer = spy(getIntegerFractionSum());
		Scientist.setEnabled(Suppliers.ofInstance(false));
		float sum = Scientist.science("enabled experiment", new CompareFloatAndIntSummedFractions(fractionSummer));
		assertEquals(INTEGER_FRACTION_SUM, sum, 0);
		verify(fractionSummer, never()).getIntSum();
	}

	@Test
	public void testSetResultPublisher() {
		//Setup
		Logger mockLog = mock(Logger.class);
		
		// Execute
		LogPublisher logPublisher = new LogPublisher(mockLog);
		Scientist.setResultPublisher(logPublisher);
		final FractionSummer fractionSummer = getIntegerFractionSum();
		Scientist.science("set publisher experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(floatSumSupplier(fractionSummer));
				experiment.attempt("intSummer", intSumSupplier(fractionSummer));
				// Forces the publication to be complete before returning
				experiment.setThrowOnMismatches(true);
			}
		});
		
		// Verify
		verify(mockLog).info(anyString());
	}

	@Test
	public void testUnhandledExceptionInRunIf() {
		// Setup
		@SuppressWarnings("unchecked")
		final DoubleAction<Operation, Exception> exceptionHandler = mock(DoubleAction.class);

		// Execute
		final FractionSummer fractionSummer = getIntegerFractionSum();

		Scientist.science("unhandled runIf-exception experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(floatSumSupplier(fractionSummer));
				experiment.attempt("intSummer", intSumSupplier(fractionSummer));
				experiment.runIf(throwsUnsupportedOperationExceptions());

				// This won't handle the exception since the Operation doesn't match
				experiment.thrown(exceptionHandler);
			}
		});
		
		// Verify
		verify(exceptionHandler).apply(eq(Operation.RUN_IF), any(UnsupportedOperationException.class));
	}

	/** Utility {@link Supplier} that always throws an exception. */
	private Supplier<Boolean> throwsUnsupportedOperationExceptions() {
		return new Supplier<Boolean>() {
			@Override
			public Boolean get() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testPublicationException() {
		// Setup
		final DoubleAction<Operation, Exception> exceptionHandler = mock(DoubleAction.class);

		LogPublisher logPublisher = mock(LogPublisher.class);
		doThrow(new UnsupportedOperationException()).when(logPublisher).publish(any(Result.class));
		
		// Execute
		Scientist.setResultPublisher(logPublisher);
		final FractionSummer fractionSummer = getIntegerFractionSum();
		Scientist.science("publishing fails experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(floatSumSupplier(fractionSummer));
				experiment.attempt("intSummer", intSumSupplier(fractionSummer));
				// Forces the publication to be complete before returning
				experiment.setThrowOnMismatches(true);
				experiment.thrown(exceptionHandler);
			}
		});
		
		// Verify
		verify(exceptionHandler).apply(eq(Operation.PUBLISH), any(UnsupportedOperationException.class));
	}

	@Test
	public void testMismatchedExperimentsWithoutThrownException() {
		final FractionSummer fractionSummer = getFloatFractionSum();
		
		Scientist.science("mismatched results experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(floatSumSupplier(fractionSummer));
				experiment.attempt("intSummer", intSumSupplier(fractionSummer));
				experiment.setThrowOnMismatches(false);
			}
		});
	}

	@Test(expected = MismatchException.class)
	public void testExceptionDueToMismatchedExperiments() {
		final FractionSummer fractionSummer = getFloatFractionSum();
		
		Scientist.science("mismatched results experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(floatSumSupplier(fractionSummer));
				experiment.attempt("intSummer", intSumSupplier(fractionSummer));
				experiment.setThrowOnMismatches(true);
			}
		});
	}

	@Test
	@Ignore("This is broken")
	public void testIgnoreMismatchedExperiment() {
		// Setup
		@SuppressWarnings("unchecked")
		final DoubleFunction<Float, Float, Boolean> ignoreMismatches = mock(DoubleFunction.class);
		when(ignoreMismatches.apply(anyFloat(), anyFloat())).thenReturn(true);
		
		// Execute
		final FractionSummer fractionSummer = getFloatFractionSum();
		
		Scientist.science("ignored mismatched results experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(floatSumSupplier(fractionSummer));
				experiment.attempt("intSummer", intSumSupplier(fractionSummer));
				experiment.setThrowOnMismatches(true);
				experiment.ignore(ignoreMismatches);
			}
		});
		
		// Verify
		verify(ignoreMismatches).apply(anyFloat(), anyFloat());
	}
}
