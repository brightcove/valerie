package val

import groovy.transform.InheritConstructors
import groovy.transform.PackageScope

/**
 * A function evaluates input and returns the ResultMap output.
 *
 * This is the core class in the library, representing the underlying concept of
 * constructing/defining a reusable and declarative function which will
 * validate the input argument.
 *
 * As this provides a Single Abstract Method (SAM), Closures will automatically
 * be coerced into this type so the standard use will be to use Closures which
 * will be coerced upon return/assignment.

 * The use of trait is largely based on Java 8 default methods. When run on
 * Java 8 much of this could
 * be adjusted to use some of the new facilities. This could also be made an
 * abstract class to be compatible with earlier Java versions if desired.
 */
trait Check {

    /**
     * Evaluate input using this constructed Check and return output ResultMap
     * @param input Input value which is to be evaluated
     * @return ResultMap output from evaluating input using this Check
     */
    abstract ResultMap call(Object input)

    /**
     * Returns a composed Check that represents a short-circuiting logical AND
     * of this Check and another.
     * When evaluating the composed Check, if this Check returns a non-passed
     * ResultMap, then the other Check is not evaluated.
     * @param other a Check that will be logically-ANDed with this Check
     * @return Returns a composed Check that represents a short-circuiting
     * logical AND of this Check and another.
     */
    Check and(Check other) {
        new AndCheck(this, other)
    }

    /**
     * Returns a composed Check that represents a short-circuiting logical OR
     * of this Check and another.
     * When evaluating the composed Check, if this Check returns
     * ResultMap.passed()
     * then the other Check is not evaluated. If none of the Checks return a
     * ResultMap.passed() then return
     * the output of the final Check
     * @param other a Check that will be logically-ORed with this Check
     * @return a composed Check that represents the short-circuiting logical
     * OR of this Check and the other Check
     */
    Check or(Check other) {
        new OrCheck(this, other)
    }

    /**
     * Compose Check which returns the combined results of the two addends
     * When evaluating the composed Check, evaluate all contained Checks and
     * return merged ResultMap
     * @param other the Check to compose with the current Check
     * @return the new composed Check
     */
    Check plus(Check other) {
        new AllCheck(other, this)
    }
}

/**
 * Represents multiple Checks combined into one using a contained sequence.
 * The abstract class provides some helper functionality to the classes in
 * this module to allow for
 * the composed Check to merge and flatten the contained Checks. As indicated
 * by the visibility, this should _not_ be considered part of the API, used
 * outside of this module, or used for polymorphism.
 */
@PackageScope
abstract class AbstractComposedCheck implements Check {
    List<? extends Check> members

    /**
     * Simple constructor where the passed members compose the new Check
     * @param members Checks from which this Check will be composed
     */
    protected AbstractComposedCheck(List<? extends Check> members) {
        this.members = members
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
        members = merge(left, right)
    }

    //The functions below allow for merging and flattening composed Check
    //The flat sequence is used rather than a binary tree structure to handle
    // a wider range of use cases
    //and theoretically a greater opportunity for optimization
    protected List<? extends Check> merge(Check left, Check right) {
        forMerge(left) + forMerge(right)
    }
    private List<? extends Check> forMerge(Check check) {
        getClass().isInstance(check) ? ((AbstractComposedCheck) check).members
                                     : [check]
    }
}

/**
 * Evaluate contained Checks in sequence using a short-circuited logical AND
 */
@InheritConstructors
class AndCheck extends AbstractComposedCheck {
    @Override
    ResultMap call(Object input) {
        ResultMap result = ResultMap.passed()
        for (Check check: members){
            result = check(input)
            if (result != ResultMap.passed()) return result
        }
        result
    }
}

/**
 * Evaluate contained Checks in sequence using a short-circuited logical OR
 */
@InheritConstructors
class OrCheck extends AbstractComposedCheck {
    @Override
    ResultMap call(Object input) {
        ResultMap result
        for (Check check: members){
            result = check(input)
            if (result == ResultMap.passed()) return result
        }
        result
    }
}

/**
 * Evaluate all contained Checks in sequence and returned merged results
 */
@InheritConstructors
class AllCheck extends AbstractComposedCheck {
    @Override
    ResultMap call(Object input) {
        ResultMap merged = ResultMap.passed()
        for (Check check: members){
            merged += check(input)
        }
        merged
    }
}
