package val;

import java.util.*;

import org.checkerframework.checker.nullness.qual.*;

/**
 * A {@link TransformerCheck} which transforms the input by
 * traversing to a child of the originally provided input.
 * If the original input is tree (which is expected) then this
 * class allows navigation through the tree so individual {@link Check}s
 * can target individual values or relevant subtrees.
 *
 * Presently this supports inputs of type {@link Map} or
 * {@link Map.Entry}. Further types can be added as needed though
 * a metada should likely be cached if reflection is introduced.
 */
public class ChildTraverser extends TransformerCheck {
    protected final String childName;

    /**
     * Construct ChildTraverser which will pass the {@code childName}
     * child of input to {@code nestedCheck}.
     * @param childName Name of child within input which should become the
     * input for {@link nestedCheck}.
     * @param nestedCheck {@link Check} which will be passed transformed input.
     */
    public ChildTraverser(String childName, Check nestedCheck) {
        super(nestedCheck);
        if (childName == null) throw new NullPointerException("childName cannot be null");
        this.childName = childName;
    }

    /**
     * Retrieves {@code childName} child from {@code input}.
     * Returns null if input is null or returned child is null.
     * @throws IllegalArgumentException if input is not null
     * and not a {@link Map} and not a {@link Map.Entry}.
     * @param input input which was passed to {@link #call} in this {@link Check}.
     * @return nullable {@code childName} from {@input}
     * which will be passed to {@code nestedCheck}.
     */
    @Override
    public @Nullable Object transform(@Nullable Object input) {
        if (input == null) return null;
        if (input instanceof Map)
            return ((Map) input).get(childName);
        if (childName.equals("value") &&
            (input instanceof Map.Entry))
            return ((Map.Entry) input).getValue();
        throw new IllegalArgumentException();
    }
}
