package val;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A Single Abstract Method (SAM) interface to facilitate
 * conversion of lambdas and similar first class functions.
 *
 * @param T The type of value to be returned,
 *          This will be {@link ResultMap} when used in creation of {@link Check}s.
 **/
public interface CheckFunk<T> {

    /**
     * Invoke this object with the provided input and context.
     *
     * @param input The input value to evaluate.
     * @param context Shared context for the evaluation of the
     *                current input graph.
     * @return The result of evaluating the provided parameters.
     *         When used to create a {@link Check}, this will be a {@link ResultMap}. 
     **/
    T call(@Nullable Object input, EvalContext context);
}
