package val;

import java.util.*;

/**
 * A function evaluates input and returns the ResultMap output.
 *
 * This is the core class in the library, representing the underlying concept of
 * constructing/defining a reusable and declarative function which will
 * validate the input argument.
 *
 * As this provides a Single Abstract Method (SAM), Closures will automatically
 * be coerced into this type so the standard use will be to use Closures which
 * will be coerced upon return/assignment.

 * The use of trait is largely based on Java 8 default methods. When run on
 * Java 8 much of this could
 * be adjusted to use some of the new facilities. This could also be made an
 * abstract class to be compatible with earlier Java versions if desired.
 */
public abstract class Check {

    public static Check from(CheckFunk<ResultMap> funk) {
        return new Check() {
            @Override
            public ResultMap call(Object input,
                                  EvalContext ctx) {
                return funk.call(input, ctx);
            }
        };
    }

    private Map<String, Object> mold = new HashMap<>();
    public Map<String, Object> getMold() {
        return mold;
    }
    public void setMold(Map<String, Object> mold) {
        this.mold = mold;
    }

    /**
     * Evaluate input using this constructed Check and return output ResultMap
     * @param input Input value which is to be evaluated
     * @return ResultMap output from evaluating input using this Check
     */
    public abstract ResultMap call(Object input, EvalContext ctx);

    /**
     * Returns a composed Check that represents a short-circuiting logical AND
     * of this Check and another.
     * When evaluating the composed Check, if this Check returns a non-passed
     * ResultMap, then the other Check is not evaluated.
     * @param other a Check that will be logically-ANDed with this Check
     * @return Returns a composed Check that represents a short-circuiting
     * logical AND of this Check and another.
     */
    public Check and(Check other) {
        return new AndCheck(this, other);
    }

    /**
     * Returns a composed Check that represents a short-circuiting logical OR
     * of this Check and another.
     * When evaluating the composed Check, if this Check returns
     * ResultMap.passed()
     * then the other Check is not evaluated. If none of the Checks return a
     * ResultMap.passed() then return
     * the output of the final Check
     * @param other a Check that will be logically-ORed with this Check
     * @return a composed Check that represents the short-circuiting logical
     * OR of this Check and the other Check
     */
    public Check or(Check other) {
        return new OrCheck(this, other);
    }

    /**
     * Compose Check which returns the combined results of the two addends
     * When evaluating the composed Check, evaluate all contained Checks and
     * return merged ResultMap
     * @param other the Check to compose with the current Check
     * @return the new composed Check
     */
    public Check plus(Check other) {
        return new AllCheck(other, this);
    }
}
