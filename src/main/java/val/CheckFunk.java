package val;

/**
 * A Single Abstract Method interface to facilitate coercion of
 * lambdas and similar first class functions.
 * @param T The type of value to be returned,
 * will normally be a {@link ResultMap}.
 */
public interface CheckFunk<T> {
    /**
     * Invoke this object with the provided input and context.
     * @param input The input value to evaluate.
     * @param context Shared context for the evaluation
     * of the current input graph.
     * @return The result of evaluating the provided parameters.
     * Will be a {@link ResultMap} in cases where this is used
     * to create a {@link Check}
     */
    T call(Object input, EvalContext context);
}
