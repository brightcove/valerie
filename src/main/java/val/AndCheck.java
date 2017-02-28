package val;

import java.util.*;

/**
 * Evaluate contained Checks in sequence using a short-circuited logical AND
 */
public class AndCheck extends AbstractComposedCheck {

    public AndCheck(List<? extends Check> members) {
        super(members);
    }
    public AndCheck(Check left, Check right) {
        super(left, right);
    }

    @Override
    public ResultMap call(Object input, EvalContext ctx) {
        ResultMap result = ResultMap.passed();
        for (Check check: members) {
            result = check.call(input, ctx);
            if (!(ResultMap.passed().equals(result))) return result;
        }
        return result;
    }
}
