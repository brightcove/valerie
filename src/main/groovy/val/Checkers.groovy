package val

import java.util.regex.Pattern

/**
 * The functional building blocks for Valerie. The methods in this class
 * should contain all of the validation
 * logic used by the library and can provide a complete implementation if the
 * consuming code assumes more
 * responsibility/noise.
 *
 * This class is stateless and purely functional. Everything remains instance
 * based for possible benefits around
 * testing and general future proofing (such as allowing configuration). The
 * functionality in this class should also be language agnostic.
 */
class Checkers {

    /**
     * Validate that the value of input is <= (lte) max
     * @param max Maximum (inclusive) value for input
     * @param mold Mold for ResultMap if input is greater than max
     * @return Check to validate input has a value less than or equal to max
     */
    Check hasValueLte(Map mold=[:], Object max) {
        mold = [msg:"should not be greater than ${max}",
                code:Result.CODE_ILLEGAL_VALUE] + mold
        satisfies(mold) { input, ctx ->
            input == null || input <= max
        }
    }

    /**
     * Validate that input is an instance of the provided type
     * @param type Type that input should be/implement/extend
     * @param mold Mold for ResultMap if input is not a type instance
     * @return Check to validate that input is an instance of type
     */
    Check isInstanceOf(Map mold=[:], Class<?> type) {
        mold = [msg:"is not of type ${type.simpleName}",
                code:Result.CODE_ILLEGAL_VALUE] + mold
        satisfies(mold) {input, ctx ->
            input == null || type.isInstance(input)
        }
    }

    /**
     * Validate that input is one of the set of allowed values provided.
     * @param allowed The set of allowed values in which input must belong
     * @param mold Mold for ResultMap if input is not in allowed
     * @return Check to validate that input is a member of the allowed set
     */
    Check isOneOf(Map mold=[:], Collection allowed) {
        mold = [msg:"is not one of allowed values: ${allowed}",
        code:Result.CODE_ILLEGAL_VALUE] + mold
        Set allowedSet = allowed as Set
        satisfies(mold) { input, ctx ->
            allowedSet.contains(input)
        }
    }

    /**
     * Validate that input is a possible value for enumClass
     * @param enumClass Enum class for which input must be a valid value
     * @param mold Mold for Result if input is not in enumClass
     * @return Check to validate that input is a possible value for enumClass
     */
    Check isOneOf(Map mold=[:], Class<? extends Enum> enumClass) {
        //Support enum references and String representations
        Set<Enum> enums = EnumSet.allOf(enumClass)
        Set strings = enums*.toString()
        isOneOf([msg:"is not one of allowed values: ${strings}"]+mold,
                strings + enums)
    }

    /**
     * Validate that input is not null
     * @param mold Mold for ResultMap when input is null
     * @return Check to validate that input is not null
     */
    Check isNotNull(Map mold=[:]) {
        mold = [msg:'required field cannot be null',
                code:Result.CODE_REQUIRED_FIELD] + mold
        satisfies(mold) { input, ctx -> input != null }
    }

    /**
     * Validate that input is null
     * This Check is not expected to be widely useful outside of things like
     * branching and may be replaced by something more general such as isEmpty
     * @param mold Mold used to create ResultMap when !null
     * @return Check to validate that input is null
     */
    Check isNull(Map mold=[:]) {
        mold = [msg:'field must be null',
                code:Result.CODE_ILLEGAL_VALUE] + mold
        satisfies(mold) { input, ctx -> input == null }
    }

    /**
     * Validate that the string representation of input matches the pattern
     * specified (implicitly anchored)
     *
     * This will match using implicit stringifying (toString)
     * so things like arrays _could_ be tested (but probably shouldn't)
     * @param pattern String representation of regular expression to match
     * @param mold Mold for ResultMap if input does not match
     */
    Check matchesRe(Map mold=[:], String pattern) {
        mold = [msg:'does not match required pattern',
                code:Result.CODE_ILLEGAL_VALUE] + mold
        Pattern regex = ~pattern
        satisfies(mold) { input, ctx ->
            input == null || input.toString().matches(regex)
        }
    }

    //
    //Generalized building block type functions
    //
    /**
     * Invokes predicate function test with input and if the test returns false
     * returns a ResultMap containing an entry with a key of key and a Result
     * having message and code
     * @param test Closure which accepts the input and returns a Boolean to
     * determine whether to call onFail
     * @param mold Mold used to create ResultMap when !test
     */
    Check satisfies(Map mold=[:], Closure<Boolean> test) {
        satisfies(test) { input, ctx  ->
            if (!mold.key)
                throw new NullPointerException('key must be provided');
            ResultMap.from( (mold.key.toString()):
                            [new Result(mold.msg.toString(),
                                        mold.code.toString())] )
        }
    }

    /**
     * Invokes predicate function test passing input as an argument and
     * returns onFail result if non-truthy
     * otherwise ResultMap.passed()
     * @param test Closure which returns a Boolean based on the value to
     * determine whether to call onFail
     * @param onFail Closure which takes the input and returns the ResultMap
     * which should indicate test failure
     * @return Check to evaluate input
     */
    Check satisfies(Map mold = [:],
                    Closure<Boolean> test,
                    Closure<ResultMap> onFail) {
        { input, ctx ->
            test(input, ctx) ? ResultMap.CLEAN : onFail(input, ctx) }
    }

    //
    // COMBINATORS
    // Used to return a new check that is a combined version of provided checks
    // All other functions should not expect collections of closures as client
    // code can use the appropriate combinator
    //
    /**
     * Combine sequence of provided checks, execute all of them, and return
     * merged results
     * @param rules Sequence of Checks, all of which wil be executed
     * @return Composed Check to evaluate input and return the merged ResultMap
     */
    Check all(Check... checks) { checks.inject { a, b -> a.plus(b) } }

    /**
     * Combine sequence of provided checks with a short circuited logical `and`
     * If any check returns a non passing ResultMap, then stop iterating and
     * return that ResultMap
     * @param checks Sequence of Checks to evaluate in order
     * @return Composed Check to evaluate input and return the first
     * non-passing ResultMap else ResultMap.passed()
     */
    Check and(Check... checks) { checks.inject { a, b -> a.and(b) } }

    /**
     * Combine sequence of provided checks with a short circuited logical 'or'
     * If any check returns a passing ResultMap, than stop iterating and return
     * that ResultMap
     * If no checks return a passing ResultMap, then return the last ResultMap
     * @param checks Sequence of Checks to evaluate in order
     * @return Composed Check to evaluate input and return the first
     * ResultMap.passed() or last non-passing
     */
    Check or(Check... checks) { checks.inject { a, b -> a.or(b) } }

    //
    // OTHER HIGHER ORDER FUNCTIONS
    //
    /**
     * If testCheck(input) passes then evaluate bodyCheck(input) and return the
     * result, if testCheck(input) fails then return ResultMap.passed().
     * The results of testCheck are only used to test above and are not returned
     * @param testCheck If passing then evaluate bodyCheck
     * @param bodyCheck Evaluated and the results returned when testCheck passes
     * @return Check which will optionally evaluate input with bodyCheck
     */
    Check when(Check testCheck,
               Check bodyCheck) {
        { input, ctx ->
            testCheck(input, ctx) == ResultMap.CLEAN ? bodyCheck(input, ctx)
                                                     : ResultMap.CLEAN }
    }

    /**
     * If testCheck(input) passes then evaluate bodyCheck and return the result,
     * if testCheck(input) fails then return ResultMap.passed().
     * The results of testCheck are only used to test above and are not returned
     * @param testCheck If passing then evaluate bodyCheck
     * @param bodyCheck Evaluated and the results returned when testCheck passes
     * @return Check which will optionally evaluate input with bodyCheck
     */
    Check unless(Check testCheck,
                 Check bodyCheck) {
        { input, ctx ->
            testCheck(input, ctx) != ResultMap.CLEAN ? bodyCheck(input, ctx)
                                                     : ResultMap.CLEAN }
    }

    /**
     * Equivalent of a logical not but based on the ResultMap returned
     * Invert the Result by:
     * If check returns a ResultMap.passed() then return a
     * ResultMap constructed out of the arguments,
     * otherwise return a ResultMap.passed()
     *
     * This seems a little ugly and may be subject to change or removal.
     * A more reusable and less awkward approach may be something like
     * (when(check, fail()))
     * This is oriented towards use in testChecks as part of when/unless/etc.
     * @param check Check for which the Result will be inverted
     * @param mold Mold for ResultMap if inner Check passed
     * @return Check to evaluate and invert result
     */
    Check not(Map mold=[:], Check check) {
      mold = [key:'not',
              msg:'condition satisfied which should not have been',
              code:'NEGATED_CHECK'] + mold
        { input, ctx ->
            check(input, ctx) == ResultMap.CLEAN ? ResultMap.from(
              [(mold.key.toString()): [new Result(mold.msg.toString(),
                                                  mold.code.toString())]])
                                               : ResultMap.CLEAN }
    }

}
