package dk.darknight.scientist.util;

import com.google.common.base.Preconditions;

import lombok.NonNull;
import lombok.Value;

public class FractionSummer {
	@Value
	private static class Fraction {
		int numerator;
		int denominator;

		public float toFloat() {
			return (float) numerator / (float) denominator;
		}

		public int toInt() {
			return numerator / denominator;
		}
	}

	private final Fraction[] fractions;

	public FractionSummer(@NonNull int[] numerators, @NonNull int[] denominators) {
		Preconditions.checkArgument(numerators.length == denominators.length,
				"'numerators' and 'denominators' must have the same length");
		fractions = new Fraction[numerators.length];
		for (int i = 0; i < fractions.length; i++) {
			fractions[i] = new Fraction(numerators[i], denominators[i]);
		}
	}

	public float getFloatSum() {
		float sum = 0;
		for (Fraction fraction : fractions) {
			sum += fraction.toFloat();
		}
		return sum;
	}

	public int getIntSum() {
		int sum = 0;
		for (Fraction fraction : fractions) {
			sum += fraction.toInt();
		}
		return sum;
	}
}
