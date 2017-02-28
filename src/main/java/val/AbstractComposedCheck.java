package val;

import java.util.*;
import java.util.stream.*;

/**
 * Represents multiple Checks combined into one using a contained sequence.
 * The abstract class provides some helper functionality to the classes in
 * this module to allow for
 * the composed Check to merge and flatten the contained Checks. As indicated
 * by the visibility, this should _not_ be considered part of the API, used
 * outside of this module, or used for polymorphism.
 */
abstract class AbstractComposedCheck extends Check {
    protected List<? extends Check> members;

    public List<? extends Check> getMembers() {
        return members;
    }
    public void setMembers(List<? extends Check> members) {
        this.members = members;
    }

    /**
     * Simple constructor where the passed members compose the new Check
     * @param members Checks from which this Check will be composed
     */
    protected AbstractComposedCheck(List<? extends Check> members) {
        this.members = members;
    }

    /**
     * Creates a composed Check out of left and right. If either argument is of
     * the same type of composed Check
     * as the one being created, then the constituent Checks will be merged and
     * flattened.
     * @param left Check which will begin the sequence contained in the
     * composed Check
     * @param right Check which will end the sequence contained in the
     * composed Check
     */
    // There's some funkiness about trying to call the above constructor within
    // this one;seeemed simple enough to leave
    protected AbstractComposedCheck(Check left, Check right) {
        members = merge(left, right);
    }

    //The functions below allow for merging and flattening composed Check
    //The flat sequence is used rather than a binary tree structure to handle
    // a wider range of use cases
    //and theoretically a greater opportunity for optimization
    protected List<? extends Check> merge(Check left, Check right) {
        return Stream.concat(forMerge(left).stream(),
                             forMerge(right).stream())
            .collect(Collectors.toList());
    }
    private List<? extends Check> forMerge(Check check) {
        return getClass().isInstance(check) ?
            ((AbstractComposedCheck) check).getMembers()
            : new ArrayList<Check>(Arrays.asList(check));
    }
}
