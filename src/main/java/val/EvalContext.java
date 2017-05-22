package val;

import java.util.*;

import org.checkerframework.checker.nullness.qual.*;

/**
 * Context for sharing data during the evaluation of a a specific input graph.
 * An instance of this class will be passed to the {@code #call} method
 * of each involved {@link Check}.
 * Each {@link Check} should be focused on validating the
 * specific input subgraph which it is provided, but the constraints
 * to apply to that input may be affected by input elsewhere
 * (such as cross field validation). This serves as a holder
 * so that during an invocation of a {@link Check} data from the
 * provided input that would not normally be visible can be accessed.
 * <p>
 * The present mechanism for providing this is through "stashed" values:
 * during evaluation values can be added to the stash which can then be
 * later used within {@link Check}s.
 * </p>
 * <p>
 * This class serves two important purposes:
 * <ul>
 * <li>
 * Provide a container for any state which is specific
 * to the current evaluation which is separated from any
 * persistent validation rules (where the rules are defined
 * in objects and instance state while the EvalContext is only
 * used as method parameters and not instance state).
 * </li>
 * <li>
 * Allow indirection between a {@link Check} and external data it may
 * require. As an example if data being validated is associated with
 * a specific state in a state machine, the {@link Check} need only
 * care about what that state is rather than how it is represented
 * elsewhere in the graph or system. Or as another example the
 * validation may depend on comparing the input provided by a user
 * with the pre-modified value of the input: the {@link Check} should
 * only be concerned with what the latter datum is and not where it
 * may be coming from. In both cases the {@link EvalContext} provides
 * an abstracted delivery mechanism for these depended upon, external
 * values.
 * </li>
 * </ul>
 * <em>Note: The present design of this class addresses concurrency concerns
 * between one evaluation and the next, however it does NOT address
 * concurrency within a single evaluation (concurrency within an
 * evaluation is not currently supported as a whole but this class
 * would be one of the few critical sections to iron out both
 * in terms of state coherence and ordered access).</em>
 */
public class EvalContext {
    /**
     * Keyed values which have been "stashed" during this evaluation.
     */
    private final Map<String, Object> stashed = new HashMap<>();

    /**
     * Returns a value that has been previously stashed in this context.
     * This is expected to be called within a {@link Check} so that
     * previously stashed data can inform the validation logic.
     * @param key The key for the stashed value to return.
     * @return The object stashed at {@code key}
     */
    @EnsuresNonNull({"#1"})
    public @Nullable Object getStashed(String key) {
        if (key == null) throw new NullPointerException("key must not be null");
        return stashed.get(key);
    }

    /**
     * Stores {@code value} at {@code key} within the stash.
     * This is expected to be called by a {@link TransformerCheck}
     * or similar side effect oriented code,
     * it should likely not be called code that is also performing
     * assertions.
     * @param key The key under which to stash this object.
     * @param value The value to store in the stash
     */
    @EnsuresNonNull({"#1", "#2"})
    public void setStashed(String key, Object value) {
        if (key == null) throw new NullPointerException("key must not be null");
        if (value == null) throw new NullPointerException("value must not be null");
        this.stashed.put(key, value);
    }
}
