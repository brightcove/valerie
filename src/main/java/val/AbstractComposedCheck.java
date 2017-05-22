package val;

import java.util.*;
import java.util.stream.*;

import org.checkerframework.checker.nullness.qual.*;

/**
 * Represents multiple Checks combined into one using a contained sequence.
 * The abstract class provides some helper functionality to the classes in
 * this module to allow for
 * the composed Check to merge and flatten the contained Checks. As indicated
 * by the visibility, this should _not_ be considered part of the API, used
 * outside of this module, or used for polymorphism.
 */
abstract class AbstractComposedCheck extends Check {
    protected final List<? extends Check> members;

    /**
     * Get the Checks from which this Check is composed.
     * Each member may also be a composed Check if they have
     * different associated logic or if they have not been merged.
     * This getter is only intended to be used for additional composition.
     * @return the constituent {@link Check}s of this composed Check
     */
    List<? extends Check> getMembers() {
        return members;
    }

    /**
     * Simple constructor where the passed members compose the new Check
     * @param members Checks from which this Check will be composed
     */
    @EnsuresNonNull({"#1"})
    protected AbstractComposedCheck(List<? extends Check> members) {
        if (members == null) throw new NullPointerException("members must not be null");
        this.members = members;
    }

    /**
     * Returns a potentially flattened representation of two {@link Check}s,
     * for use in an AbstractComposedCheck of type {@code clazz}.
     * If either provided {@link Check} is of the same type as
     * {@code clazz} then its members will be extracted from their containing
     * {@link Check} and added directly to the returned list.
     * For the standard AbstractComposedCheck types this allows for a logically
     * equivalent structure which is simpler and facilitates further
     * optimizations.
     * @param left Check providing the beginning of the created sequence.
     * @param right Check providing the end of the created sequence.
     * @return a list of {@link Check}s which when used to construct an
     * AbstractComposedCheck should have the same behavior as composing the
     * two provided {@link Check}s.
     */
    static List<? extends Check> merge(Check left, Check right,
                                       Class<? extends AbstractComposedCheck> clazz) {
        return Stream.concat(forMerge(left, clazz).stream(),
                             forMerge(right, clazz).stream())
            .collect(Collectors.toList());
    }

    /**
     * Used within {@link merge} to extract members from {@code check} if
     * {@code check} is of type {@code clazz}, otherwise return
     * {@code check} within a List.
     * @param check Check from which members may be extracted.
     * @param clazz Type of {@link AbstractComposedCheck} from which members
     * will be extracted.
     * @return a List of {@code check} or its members if it is of type {@code clazz}
     */
    @EnsuresNonNull({"#1", "#2"})
    private static List<? extends Check> forMerge(Check check,
                                                  Class<? extends AbstractComposedCheck> clazz) {
        if (check == null) throw new NullPointerException("check must not be null");
        if (clazz == null) throw new NullPointerException("clazz must not be null");
        return clazz.isInstance(check) ?
            ((AbstractComposedCheck) check).getMembers()
            : new ArrayList<Check>(Arrays.asList(check));
    }
}
