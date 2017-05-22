package val;

import java.util.*;

/**
 * Evaluate all contained Checks in sequence and returned merged results.
 * The output of this Check is the equivalent of adding all of the
 * member {@link Checks} (and via distribution adding the
 * {@link ResultMap}s from the evaluation of all of the {@link Checks}.
 * This composed Check will evaluate all of the {@link Check}s with
 * no short-circuiting.
 */
public class AllCheck extends AbstractComposedCheck {

    /**
     * Construct AllCheck using provided members.
     * @param members List of {@link Check}s which will all be evaluated
     * and for which added {@link ResultMap}s will be returned.
     */
    public AllCheck(List<? extends Check> members) {
        super(members);
    }

    /**
     * Construct AllCheck by merging the two provided {@link Checks}s.
     * If either argument is also an {@link AllCheck} then its members
     * will be extracted and stored directly in the new instance.
     * @param left Check from which the beginning of the new instance's
     * members will be created.
     * @param right Check from which the end of the new instance's
     * members will be created.
     */
    public AllCheck(Check left, Check right) {
        super(merge(left, right, AllCheck.class));
    }

    @Override
    public ResultMap call(Object input, EvalContext ctx) {
        ResultMap merged = ResultMap.CLEAN;
        for (Check check: members){
            merged = merged.plus(check.call(input, ctx));
        }
        return merged;
    }
}
