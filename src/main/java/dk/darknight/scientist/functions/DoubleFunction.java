package dk.darknight.scientist.functions;

/**
 * A function that takes two parameters and returns a result.
 * 
 * @param <F1>
 *            parameter one's type.
 * @param <F2>
 *            parameter two's type.
 * @param <T>
 *            the return type.
 */
public interface DoubleFunction<F1, F2, T> {
	T apply(F1 first, F2 second);
}