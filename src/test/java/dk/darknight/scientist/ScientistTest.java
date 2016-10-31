package dk.darknight.scientist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import dk.darknight.scientist.functions.ExperimentFunction;
import dk.darknight.scientist.util.FractionSummer;
import static org.mockito.Mockito.*;

public class ScientistTest {
	private static final int INTEGER_FRACTION_SUM = 7;

	@Before
	public void setup() {
		Scientist.setResultPublisher(LogPublisher.DEFAULT);
		Scientist.setEnabled(Suppliers.ofInstance(true));
	}
	
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

	@Test
	public void testScienceStringExperimentFunctionOfTTClean() {
		FractionSummer fractionSummer = getIntegerFractionSum();

		float sum = Scientist.science("singlethreaded expriment", new CompareFloatAndIntSummedFractions(fractionSummer));

		assertEquals(INTEGER_FRACTION_SUM, sum, 0);
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

	/**
	 * Return a {@link FractionSummer} that will only work with float summing.
	 */
	private FractionSummer getFloatFractionSum() {
		int[] numerators = { 1, 1, 1 };
		int[] denominators = { 3, 3, 3 };
		return new FractionSummer(numerators, denominators);
	}

	@Test
	public void testScienceStringIntExperimentFunctionOfTTClean() {
		FractionSummer fractionSummer = spy(getIntegerFractionSum());

		float sum = Scientist.science("multithreaded expriment", 2, new CompareFloatAndIntSummedFractions(fractionSummer));

		assertEquals(INTEGER_FRACTION_SUM, sum, 0);
		verify(fractionSummer).getFloatSum();
		verify(fractionSummer).getIntSum();
	}

	@Test
	public void testSetEnabled() {
		FractionSummer fractionSummer = spy(getIntegerFractionSum());
		Scientist.setEnabled(Suppliers.ofInstance(false));
		float sum = Scientist.science("singlethreaded expriment", new CompareFloatAndIntSummedFractions(fractionSummer));
		assertEquals(INTEGER_FRACTION_SUM, sum, 0);
		verify(fractionSummer, never()).getIntSum();
	}

	@Test
	public void testSetResultPublisher() {
		Logger mockLog = mock(Logger.class);
		LogPublisher logPublisher = new LogPublisher(mockLog);
		Scientist.setResultPublisher(logPublisher);		
		FractionSummer fractionSummer = getIntegerFractionSum();
		Scientist.science("singlethreaded expriment", new CompareFloatAndIntSummedFractions(fractionSummer));
		verify(mockLog).info(anyString());
	}

}
