package val;

import java.util.*;

/**
 * Evaluate all contained Checks in sequence and returned merged results
 */
public class AllCheck extends AbstractComposedCheck {

    public AllCheck(List<? extends Check> members) {
        super(members);
    }
    public AllCheck(Check left, Check right) {
        super(left, right);
    }

    @Override
    public ResultMap call(Object input, EvalContext ctx) {
        ResultMap merged = ResultMap.passed();
        for (Check check: members){
            merged = merged.plus(check.call(input, ctx));
        }
        return merged;
    }
}
