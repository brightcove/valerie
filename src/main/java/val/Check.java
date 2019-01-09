package val;

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

/**
 * A composable function which returns a {@link ResultMap} based on evaluated input.
 *
 * This is the core class in this library, representing the underlying concept of
 * defining a contstraint which input should satisfy and producing appropriate
 * feedback based on that validation. Fundamentally Checks are predicates/tests
 * which support richer responses than a binary pass/fail or true/false.
 *
 * Valerie's validation logic can be viewed as mapping an *input graph* on to a
 * *validation graph*, and beyond the basic predicate behavior Checks also serve
 * as the common type for nodes in that validation graph. That means that in addition
 * to validating input Checks may perform additional processing such as traversal.
 * The best pratice is to have all Checks either perform validation or
 * provide some such supporting logic, but **not both**.
 *
 * This is an abstract class so that the state contained within the mold can be relied
 * upon as part of the type. This precludes this class from being
 * a cross-language functional interface/Single Abstract Method (SAM) to which
 * first class functions could be transparently mapped, so
 * {@link CheckFunk} and the {@link #from} method are used to facilitate
 * explicit conversion of lambdas and company.
 **/
public abstract class Check {

    /**
     * Return a Check based on the provided {@link CheckFunk}.
     *
     * Serves as an indirect functional interface so that any compatible
     * function expression which returns a {@link ResultMap} can be easily
     * converted to a Check.
     *
     * @param funk The function which should be represented as a Check.
     * @return A Check containing the logic provided by {@code funk}.
     **/
    @EnsuresNonNull({"#1"})
    public static Check from(final CheckFunk<ResultMap> funk) {
        if (funk == null) throw new NullPointerException("funk must not be null");
        return new Check() {
            @Override
            public ResultMap call(final Object input,
                                  final EvalContext ctx) {
                return funk.call(input, ctx);
            }
        };
    }

    /**
     * Additional data to be used to produce a {@link ResultMap}.
     *
     * Think pottery, not fungus. The mold is used to contain information
     * such as where in the input graph this Check is being evaluated,
     * and therefore what value to set for the key in the {@link ResultMap}.
     **/
    private Map<String, Object> mold = new HashMap<>();

    /**
     * Get the {@code mold} associated with this Check, likely to populate a {@link ResultMap}.
     *
     * @return The {@code mold} defined for this Check.
     **/
    public Map<String, Object> getMold() { return mold; }      

    /**
     * Set the {@code mold} associated with this Check, likely during definition of the Check.
     *
     * @param mold Mold that this Check can use to populate {@link ResultMap}s.
     **/
    public void setMold(final Map<String, Object> mold) {
        if (mold == null) throw new NullPointerException("mold must not be null");
        this.mold = mold;
    }

    /**
     * Evaluate this Check against the provided {@code input} and {@code context}.
     *
     * @param input Input value to be validated.
     * @param ctx Information from the evaluation of the current input graph evaluation
     *            which may inform this Check.
     * @return Any feedback derived from the evaluation of this Check.
     **/
    public abstract ResultMap call(final Object input, final EvalContext ctx);

    /**
     * Return a composed Check that is a short-circuiting logical AND of {@code this} and {@code other}.
     *
     * The resulting Check will return the first non-clean {@link ResultMap} if any are encountered,
     * otherwise a clean {@link ResultMap}.
     *
     * @param other A Check which will be ANDed with this Check.
     * @return A Check composed of {@code this} AND {@other}.
     **/
    public Check and(final Check other) { 
        final Check left = this;
        return new Check() {
            @Override
            public ResultMap call(final Object input,
                                  final EvalContext ctx) {
                ResultMap leftResult = left.call(input, ctx);
                return leftResult.isClean() ? other.call(input, ctx)
                                            : leftResult;
            }
        };
    }

    /**
     * Return a composed Check that is a short-circuiting logical OR of {@code this} and {@code other}.
     *
     * The resulting Check will return ths first clean {@link ResultMap} if any are encountered,
     * otherwise the result of {@code other}.
     *
     * @param other A Check which will be ORed with this Check.
     * @return A Check composed of {@code this} OR {@code other}.
     **/
    public Check or(final Check other) {
        final Check left = this;
        return new Check() {
            @Override
            public ResultMap call(final Object input,
                                  final EvalContext ctx) {
                return left.call(input, ctx).isClean() ? ResultMap.CLEAN
                                                       : other.call(input, ctx);
            }
        };
    }

    /**
     * Return a composed Check that which returns the combined results of {@code this} and {@code other}.
     *
     * When evaluating the composed Check, both {@code this} and {@code other} will be evaluated and the
     * resulting {@link ResultMaps} will be added.
     * This adheres to standard mathematical properties of addition.
     * For example given two Checks a and b:
     *   a.plus(b).call(i,c) == a.call(i,c).plus(b.call(i,c)).
     *
     * @param other A Check which will be combined with this check.
     * @return A Check composed of {@code this} plus {@code other}.
     **/
    public Check plus(final Check other) {
        final Check left = this;
        return new Check() {
            @Override
            public ResultMap call(final Object input,
                                  final EvalContext ctx) {
                return left.call(input, ctx).plus(other.call(input, ctx));
            }
        };
    }
}
