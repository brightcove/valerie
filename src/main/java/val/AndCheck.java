package val;

import java.util.*;

/**
 * Evaluate contained Checks in sequence using a logical AND.
 * If any {@link ResultMap}s are not clean the first such will be returned,
 * otherwise a clean {@link ResultMap} will be returned.
 * Short-circuiting behavior should not be relied upon.
 */
public class AndCheck extends AbstractComposedCheck {

    /**
     * Construct AndCheck using provided members.
     * @param member List of {@link Check}s which will be composed
     * to create the AndCheck; order is significant.
     */
    public AndCheck(List<? extends Check> members) {
        super(members);
    }

    /**
     * Construct AndCheck by merging the two provided {@link Check}s.
     * If either argument is also an {@link AndCheck} then its members
     * will be extracted and stored directly in the instance.
     * @param left Check from which the beginning of the new instance's
     * members will be created.
     * @param right Check from which the end of the new instance's
     * members will be created.
     */
    public AndCheck(Check left, Check right) {
        super(merge(left, right, AndCheck.class));
    }

    @Override
    public ResultMap call(Object input, EvalContext ctx) {
        ResultMap result = ResultMap.CLEAN;
        for (Check check: members) {
            result = check.call(input, ctx);
            if (!(result.isClean())) return result;
        }
        return result;
    }
}
