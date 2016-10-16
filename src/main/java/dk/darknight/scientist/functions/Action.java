package dk.darknight.scientist.functions;

import com.google.common.base.Function;

/**
 * An a void callback with one parameter.
 * 
 * @param <T>
 *            the parameter's type.
 */
public interface Action<T> extends Function<T, Void> {
}
