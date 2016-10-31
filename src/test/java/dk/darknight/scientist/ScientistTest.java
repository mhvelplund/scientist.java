package dk.darknight.scientist;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;

import dk.darknight.scientist.functions.Action;
import dk.darknight.scientist.functions.DoubleAction;
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
			experiment.use(new Supplier<Float>() {
				@Override
				public Float get() {
					return fractionSummer.getFloatSum();
				}
			});

			experiment.attempt("intSummer", new Supplier<Float>() {
				@Override
				public Float get() {
					return (float) fractionSummer.getIntSum();
				}
			});
		}
	}

	private static final int INTEGER_FRACTION_SUM = 7;

	/**
	 * Return a {@link FractionSummer} that will only work with float summing.
	 */
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

	@Before
	public void setup() {
		Scientist.setResultPublisher(LogPublisher.DEFAULT);
		Scientist.setEnabled(Suppliers.ofInstance(true));
	}

	@Test
	public void testHandledExceptionInRunIf() {
		final FractionSummer fractionSummer = getIntegerFractionSum();

		Scientist.science("handled runIf-exception experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(new Supplier<Float>() {
					@Override
					public Float get() {
						return fractionSummer.getFloatSum();
					}
				});

				experiment.attempt("intSummer", new Supplier<Float>() {
					@Override
					public Float get() {
						return (float) fractionSummer.getIntSum();
					}
				});

				experiment.runIf(throwsUnsupportedOperationExceptions());

				experiment.thrown(getExceptionHandler(Operation.RUN_IF));
			}

		});
	}

	/**
	 * Return an exception handler for {@link UnsupportedOperationException}s.
	 * 
	 * @param operation
	 *           if the operation was this, then the exception is ignored,
	 *           otherwise it's propagated.
	 * @return an exception handler
	 */
	private DoubleAction<Operation, Exception> getExceptionHandler(final Operation operation) {
		return new DoubleAction<Operation, Exception>() {
			@Override
			public Void apply(Operation first, Exception second) {
				if (second instanceof UnsupportedOperationException && first.equals(operation)) {
					// OK - handled
				} else {
					throw Throwables.propagate(second);
				}

				return null;
			}
		};
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
		float sum = Scientist.science("singlethreaded experiment", new CompareFloatAndIntSummedFractions(fractionSummer));
		assertEquals(INTEGER_FRACTION_SUM, sum, 0);
		verify(fractionSummer, never()).getIntSum();
	}

	@Test
	public void testSetResultPublisher() {
		Logger mockLog = mock(Logger.class);
		LogPublisher logPublisher = new LogPublisher(mockLog);
		Scientist.setResultPublisher(logPublisher);
		FractionSummer fractionSummer = getIntegerFractionSum();
		Scientist.science("singlethreaded experiment", new CompareFloatAndIntSummedFractions(fractionSummer));
		verify(mockLog).info(anyString());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testUnhandledExceptionInRunIf() {
		final FractionSummer fractionSummer = getIntegerFractionSum();

		Scientist.science("unhandled runIf-exception experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(new Supplier<Float>() {
					@Override
					public Float get() {
						return fractionSummer.getFloatSum();
					}
				});

				experiment.attempt("intSummer", new Supplier<Float>() {
					@Override
					public Float get() {
						return (float) fractionSummer.getIntSum();
					}
				});

				experiment.runIf(throwsUnsupportedOperationExceptions());

				// This won't handle the exception since the Operation doesn't match
				experiment.thrown(getExceptionHandler(Operation.PUBLISH));
			}

		});
	}

	@Test
	public void testHandledExceptionInEnabledCheck() {
		final FractionSummer fractionSummer = getIntegerFractionSum();

		Scientist.setEnabled(throwsUnsupportedOperationExceptions());
		Scientist.science("handled enabled-exception experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(new Supplier<Float>() {
					@Override
					public Float get() {
						return fractionSummer.getFloatSum();
					}
				});

				experiment.attempt("intSummer", new Supplier<Float>() {
					@Override
					public Float get() {
						return (float) fractionSummer.getIntSum();
					}
				});

				experiment.thrown(getExceptionHandler(Operation.ENABLED));
			}

		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testUnandledExceptionInEnabledCheck() {
		final FractionSummer fractionSummer = getIntegerFractionSum();

		Scientist.setEnabled(throwsUnsupportedOperationExceptions());
		Scientist.science("unhandled enabled-exception experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(new Supplier<Float>() {
					@Override
					public Float get() {
						return fractionSummer.getFloatSum();
					}
				});

				experiment.attempt("intSummer", new Supplier<Float>() {
					@Override
					public Float get() {
						return (float) fractionSummer.getIntSum();
					}
				});

				// This won't handle the exception since the Operation doesn't match
				experiment.thrown(getExceptionHandler(Operation.IGNORE));
			}

		});
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
	public void testBeforeRun() {
		@SuppressWarnings("unchecked")
		final Action<Void> beforeRun = mock(Action.class);
		final FractionSummer fractionSummer = getIntegerFractionSum();

		Scientist.science("beforeRun experiment", new ExperimentFunction<Float, Float>() {
			@Override
			public void apply(IExperiment<Float, Float> experiment) {
				experiment.use(new Supplier<Float>() {
					@Override
					public Float get() {
						return fractionSummer.getFloatSum();
					}
				});

				experiment.attempt("intSummer", new Supplier<Float>() {
					@Override
					public Float get() {
						return (float) fractionSummer.getIntSum();
					}
				});

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
				experiment.use(new Supplier<Float>() {
					@Override
					public Float get() {
						return fractionSummer.getFloatSum();
					}
				});

				experiment.attempt("intSummer", new Supplier<Float>() {
					@Override
					public Float get() {
						return (float) fractionSummer.getIntSum();
					}
				});

				experiment.beforeRun(beforeRun);
			}
		});
	}

	@Test
	@Ignore
	public void testPublicationException() {
		fail();
	}

	@Test
	@Ignore
	public void testMismatchedExperimentsWithoutThrownException() {
		fail();
	}

	@Test(expected = MismatchException.class)
	@Ignore
	public void testExceptionDueToMismatchedExperiments() {
		fail();
	}

	@Test
	@Ignore
	public void testIgnoreMismatchedExperiment() {
		fail();
	}
}
