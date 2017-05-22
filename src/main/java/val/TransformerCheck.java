package val;

import org.checkerframework.checker.nullness.qual.*;

/**
 * An implementation of {@link Check} which presents a transformed view of
 * the input to its nested {@link Check}.
 * <p>
 * Like all {@link Check}s, the provided input should almost certainly
 * <em>not</em> be modified in any way that is unhygienic and possibly visible
 * by any code outside of its enclosed scope. The expected
 * logic is to pass a new value representing a modified perspective of the input
 * to the nested check and not make any changes by reference.
 * </p><p>
 * This class is used to traverse the input graph in an {@link Idator}.
 * </p><p>
 * Any instance of {@link TransformerCheck} should remain focused on the role
 * of providing the modified input to the nested {@link Check};
 * any validation logic/assertions should be done within
 * within the nested {@link Check} which is likely to be a composition of
 * simple {@link Check}s.
 */
abstract class TransformerCheck extends Check {
    protected final Check nestedCheck;

    /**
     * Construct a TransformerCheck which will pass a modified view of
     * its input to {@code nestedCheck}.
     * @param nestedCheck Check which will be passed the transformed
     * view of the input
     */
    @EnsuresNonNull({"#1"})
    protected TransformerCheck(Check nestedCheck) {
        if (nestedCheck == null) throw new NullPointerException("nestedCheck must not be null");
        this.nestedCheck = nestedCheck;
    }

    /**
     * Create the transfomed view of the input which will be passed to
     * {@code nestedCheck}.
     * @param input input which has been passed to {@code #call}.
     * @return A value that will be passed as the input to the {@code #call}
     * method of {@code nestedCheck}.
     */
    protected abstract @Nullable Object transform(@Nullable Object input);


    @Override
    public ResultMap call(Object input, EvalContext context) {
        return nestedCheck.call(transform(input), context);
    }
}
