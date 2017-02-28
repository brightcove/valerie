package val;

import java.util.*;

/**
 * Evaluation time context object
 * Used to hold any information which is to be shared
 * during a single evaluation
 */
public class EvalContext {
    private Map<String, Object> stashed = new HashMap<>();

    public Map<String,Object> getStashed() {
        return stashed;
    }
    public void setStashed(Map<String,Object> stashed) {
        this.stashed = stashed;
    }
}
