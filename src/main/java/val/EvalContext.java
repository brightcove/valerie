package val;

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Data shared among {@link Check}s during the evaluation of an input graph.
 *
 * An instance of this class will be passed to the {@link Check#call} method
 * of each invoked {@link Check}.
 * Each {@link Check} should be focused on validating the specific input subgraph
 * which it is provided, but some of the parameters for that validation may be
 * derived from data elsewhere in the input (i.e. cross-field validation).
 * This context serves as holder for such data that would normally be visible
 * while an individual {@link Check} is being evaluated.
 *
 * The present mechanism for sharing data is through the use of "stashed" values;
 * during evaluation values can be added to the stash which can then be accessed
 * by subsequent {@link Check}s.
 *
 * This class serves two important purposes:
 *
 * - Contain any state which is specific to the current evaluation.
 *   Data which is persistent across evaluations is retained as instance
 *   data on the heap, while instances of EvalContext are treated as
 *   shorter-lived method parameters. This helps provide a clearer separation
 *   and assists with some concurrency concerns.
 *   Note: The current concurrency focus is on the level of evaluation of an
 *   entire input graph, such as when handling an HTTP request. There is not
 *   currently support for concurrent evaluation of {@link Check}s for a
 *   single input graph: the logic associated with this class would be the major
 *   section of code to adjust to support that if needed.
 *
 * - Allow indirection between a {@link Check} and external data that it may require.
 *   As an example, a rule that involves validating input based on a previous value
 *   should be provided that previous value without needing to be concerned with its
 *   origin. This class provides an abstracted delivery mechanism for those values so
 *   that the individual {@link Check}s can remain focused and insulated from changes
 *   elsewhere in the graph or the surrounding system.
 **/
public final class EvalContext {

    /*
     * Keyed values which have been "stashed" during this evaluation.
     */
     private final Map<String, Object> stashed = new HashMap<>();

     /**
      * Return a value which has been previously stashed in this context.
      *
      * This is intended to be called while evaluating a {@link Check}
      * so that previously stashed data can inform the validation decision.
      *
      * @param key The key of the stashed value to return.
      * @return The object stashed at {@code key}, or null if no object is stashed.
      **/
     @EnsuresNonNull({"#1"})
     public @Nullable Object getStashed(String key) {
         if (key == null) throw new NullPointerException("key must not be null");
         return stashed.get(key);
     }

     /**
      * Stores {@code value} at {@code key} within the stash.
      *
      * This is expected to be called by a {@link TransformerCheck}
      * or similar mutation oriented code, it should not be called
      * by code that woud also be performing assertions.
      *
      * @param key The key under which to stash this object.
      * @param value The value to store in the stash.
      **/
      @EnsuresNonNull({"#1", "#2"})
      public void setStashed(String key, Object value) {
          if (key == null) throw new NullPointerException("key must not be null");
          if (value == null) throw new NullPointerException("value must not be null");
          this.stashed.put(key, value);
      }
}
