package val;

import java.util.*;

/**
 * Evaluate contained Checks in sequence using a logical OR.
 * If any {@link ResultMap} is clean the first such will be returned,
 * otherwise the last {@link ResultMap} will be returned.
 * Short-circuiting behavior should not be relied upon.
 */
public class OrCheck extends AbstractComposedCheck {

    /**
     * Construct OrCheck using provided members.
     * @param member List of {@link Check}s which will be composed
     * to create the OrCheck; order is significant.
     */
    public OrCheck(List<? extends Check> members) {
        super(members);
    }

    /**
     * Construct OrCheck by merging the two provided {@link Check}s.
     * If either argument is also an {@link OrCheck} then its members
     * will be extracted and stored directly in the instance.
     * @param left Check from which the beginning of the new instance's
     * members will be created.
     * @param right Check from which the end of the new instance's
     * members will be created.
     */
    public OrCheck(Check left, Check right) {
        super(merge(left, right, OrCheck.class));
    }

    @Override
    public ResultMap call(Object input, EvalContext ctx) {
        ResultMap result = ResultMap.CLEAN;
        for (Check check: members) {
            result = check.call(input, ctx);
            if (result.isClean()) return result;
        }
        return result;
    }
}
