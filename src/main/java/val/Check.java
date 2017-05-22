package val;

import java.util.*;

import org.checkerframework.checker.nullness.qual.*;

/**
 * A composable function that evaluates input and returns a {@link ResultMap}
 * based on that input and the values provided in the {@link #mold}
 * property.
 * <p>
 * This is the core class in the library, representing the underlying concept of
 * constructing/defining a reusable and declarative function which will
 * validate the input argument.
 * </p>
 * <p>
 * This is an abstract class so that the state contained within the mold can
 * be relied upon as part of the type. This precludes this class from being
 * a cross-language functional interface/Single Abstract Method(SAM) to which
 * first class functions could be transparently mapped, so
 * {@link CheckFunk} and the {@link #from} method are used to facilitate
 * explicit conversion of lambdas, etc.
 * </p>
 */
public abstract class Check {
    /**
     * Construct a Check out of the provided {@link CheckFunk}.
     * Provides an indirect functional interface so that lambda expressions
     * and similar first class functions which return {@link ResultMap}s can
     * be easily converted to a Check.
     */
    @EnsuresNonNull({"#1"})
    public static Check from(CheckFunk<ResultMap> arg) {
        if (arg == null) throw new NullPointerException("arg must not be null");
        CheckFunk<ResultMap> funk = arg;
        return new Check() {
            @Override
            public ResultMap call(Object input,
                                  EvalContext ctx) {
                return funk.call(input, ctx);
            }
        };
    }

    /**
     * Additional context used in the generation of a {@link ResultMap}.
     * Think pottery, not fungus. The mold is used to contain information
     * such as where in the input graph this Check is being evaluated,
     * and therefore what value to set for the key in the {@link ResultMap}.
     */
    private Map<String, Object> mold = new HashMap<>();

    /**
     * Get the mold associated with this Check, likely during construction of
     * a {@link ResultMap}.
     * @return The mold for the {@link ResultMap}s returned by this Check.
     */
    public Map<String, Object> getMold() {
        return mold;
    }

    /**
     * Set the mold associated with this Check, should be set as part of the
     * definition of this Check.
     * @param mold Mold that is available for this Check to use in generating
     * a {@link ResultMap}.
     */
    @EnsuresNonNull({"#1"})
    public void setMold(Map<String, Object> mold) {
        if (mold == null) throw new NullPointerException("mold must not be null");
        this.mold = mold;
    }

    /**
     * Evaluate {@code input} using this Check and the provided {@code context}.
     * @param input Input value which is to be evaluated.
     * @param ctx {@link EvalContext} containing additional evaluation time information
     * which may inform this Check.
     * @return {@link ResultMap} output from evaluating input using this Check.
     */
    public abstract ResultMap call(Object input, EvalContext context);

    /**
     * Returns a composed Check that represents a logical AND
     * of this Check and another.
     * The composed Check will return the first non-clean
     * {@link ResultMap} if any are encountered, otherwise a clean
     * {@link ResultMap}.
     * @param other a Check that will be logically-ANDed with this Check
     * @return Returns a composed Check that represents a logical
     * AND of this Check and another.
     */
    public Check and(Check other) {
        return new AndCheck(this, other);
    }

    /**
     * Returns a composed Check that represents a logical OR
     * of this Check and another.
     * The composed Check will return the first clean {@link ResultMap}
     * encountered, otherwise the result of {@code other}.
     * @param other A Check that will be logically-ORed with this Check.
     * @return A composed Check that represents the short-circuiting logical
     * OR of this Check and the other Check.
     */
    public Check or(Check other) {
        return new OrCheck(this, other);
    }

    /**
     * Compose Check which returns the combined results of the two addends
     * When evaluating the composed Check, evaluate all contained Checks and
     * return merged {@link ResultMap}.
     * Checks should be distributive in addition such that given two checks a and b:
     * (a.plus(b)).call(input) == (a.call(input) + b.call(input))
     * @param other The Check to compose with the current Check.
     * @return The new composed Check.
     */
    public Check plus(Check other) {
        return new AllCheck(other, this);
    }
}
