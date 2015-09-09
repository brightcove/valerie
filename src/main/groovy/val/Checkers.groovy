package val

import java.util.regex.Pattern

/**
 * The functional building blocks for Valerie. The methods in this class should contain all of the validation
 * logic used by the library and can provide a complete implementation if the consuming code assumes more
 * responsibility/noise.
 *
 * This class is stateless and purely functional. Everything remains instance based for possible benefits around
 * testing and general future proofing (such as allowing configuration). The functionality
 * in this class should also be language agnostic.
 */
class Checkers {

    /**
     * Return ResultMap constructed from provided arguments
     * @param key Key for Result
     * @param message Message for Result
     * @param code Code for Result
     * @return Check which will return ResultMap with constructed Result
     */
    Check fail(String key = 'fail',
               String message = 'failed because you said so',
               String code = val.Result.CODE_ILLEGAL_VALUE) {
        {input -> ResultMap.from((key): [new Result(message, code)])}
    }

    /**
     * Validate that the input contains a member with the specified name, regardless of value
     * @param member The name of the member that input should contain
     * @param key Key for Result if member does not exist
     * @param message Message for Result if member does not exist
     * @param code Code for Result if member does not exist
     * @return Check to validate that input contains specified member
     */
    Check hasMember(String member,
                    String key,
                    String message = "required field ${member} is not present",
                    String code = val.Result.CODE_REQUIRED_FIELD) {
        satisfies({input ->
            input?.containsKey(member)
        }, key, message, code)
    }

    /**
     * Validates that map only contains fields that are in the Set of fields provided
     * @param fields Set of field names from which input fields should be members
     * @param key Key prefix which will have input field name appended (`.` separated) if input field is not in fields
     * @param message Message for Result for each field not present in fields
     * @param code Code for Result for each field not present in fields
     * @return Check which will validate field membership
     */
    Check hasOnlyFieldsIn(Set<String> fields,
                          String key,
                          String message = 'field is unknown',
                          String code = val.Result.CODE_ILLEGAL_FIELD) {
        { input ->
            if (!(input instanceof Map))
                return val.ResultMap.from((key): [new val.Result('only maps are supported', 'ILLEGAL_VALUE')])
            Map collectedResults = [:]
            ((Map) input).keySet().each{ fieldName ->
                if (!fields.contains(fieldName)) {
                    collectedResults["${key?key+'.':''}${fieldName}"] = [new val.Result(message, code)]
                }
            }
            val.ResultMap.from(collectedResults)
        }
    }

    /**
     * Validates that map only contains fields that are declared for the Class provided
     * @param targetClass Class whose delcared fields will become the Set of valid fields
     * @param key Key prefix which will have input field name appended (`.` separated)
     * if input field is not valid for targetClass
     * @param message Message for Result for each field not valid for targetClass
     * @param code Code for Result for each field not valid for targetClass
     * @return Check which will validate field membership
     */
    Check hasOnlyFieldsIn(Class targetClass,
                          String key,
                          String message = 'field is unknown',
                          String code = val.Result.CODE_ILLEGAL_FIELD) {
        hasOnlyFieldsIn(new HashSet<>(targetClass.getDeclaredFields()*.name), key, message, code)
    }

    /**
     * Validate that the size of input is >= (gte) the min size.
     * Uses Groovy size duckiness
     * @param min Minimum (inclusive) size for input
     * @param key Key for Result if input is smaller than min
     * @param message Message for Result if input is smaller than min
     * @param code Code for Result if input is smaller than min
     * @return Check to validate that input has a size greater than or equal to min
     */
    Check hasSizeGte(Integer min,
                     String key,
                     String message = "should be on least ${min} long",
                     String code = val.Result.CODE_TOO_SHORT) {
        satisfies({input ->
            input == null || input?.size() >= min}, key, message, code)
    }

    /**
     * Validate that the size of input is <= (lte) the max size.
     * Uses Groovy size duckiness
     * @param max Maximum (inclusive) size for input
     * @param key Key for Result if input is larger than max
     * @param message Message for Result if input is larger than max
     * @param code Code for Result if value is input than max
     * @return Check to validate that input has a size less than or equal to max
     */
    Check hasSizeLte(Integer max,
                     String key,
                     String message = "should be no longer than ${max}",
                     String code = val.Result.CODE_TOO_LONG) {
        satisfies({input ->
            input?.size() <= max}, key, message, code)
    }

    /**
     * Validate that the value of input is >= (gte) min
     * @param min Minimum (inclusive) value for input
     * @param key Key for Result if input is less than min
     * @param message Message for Result if input is less than min
     * @param code Code for Result if input is less than min
     * @return Check to validate that input has a value greater than or equal to min
     */
    Check hasValueGte(Object min,
                      String key,
                      String message = "should not be less than ${min}",
                      String code = val.Result.CODE_ILLEGAL_VALUE) {
        satisfies({input ->
            input == null || input >= min}, key, message, code)
    }

    /**
     * Validate that the value of input is <= (lte) max
     * @param max Maximum (inclusive) value for input
     * @param key Key for Result if input is greater than max
     * @param message Message for Result if input is greater than max
     * @param code Code for Result if input is greater than max
     * @return Check to validate that input has a value less than or equal to max
     */
    Check hasValueLte(Object max,
                      String key,
                      String message = "should not be greater than ${max}",
                      String code = val.Result.CODE_ILLEGAL_VALUE) {
        satisfies({input ->
            input == null || input <= max}, key, message, code)
    }

    /**
     * Validate that input is an instance of the provided type
     * @param type Type that input should be/implement/extend
     * @param key Key for Result if input is not an instance of type
     * @param message Message for Result if input is not an instance of type
     * @param code Code for Result if input is not an instance of type
     * @return Check to validate that input is an instance of type
     */
    Check isInstanceOf(Class<?> type,
                   String key,
                   String message = "is not of type ${type.simpleName}",
                   String code = val.Result.CODE_ILLEGAL_VALUE) {
        satisfies({input ->
            input == null || type.isInstance(input) }, key, message, code)
    }

    /**
     * Validate that input is one of the set of allowed values provided.
     * @param allowed The set of allowed values for which input should be a member
     * @param key Key for Result if input is not one of the allowed values
     * @param message Message for Result if input is not one of the allowed values
     * @param code Code for Result if input is not one of the allowed values
     * @return Check to validate that input is a member of the allowed set
     */
    Check isOneOf(Collection allowed,
                  String key,
                  String message = "is not one of allowed values: ${allowed}",
                  String code = val.Result.CODE_ILLEGAL_VALUE) {
        Set allowedSet = allowed as Set
        satisfies({input ->
            allowedSet.contains(input)}, key, message, code)
    }

    /**
     * Validate that input is of type enumClass or is one of the possible values for enumClass
     * @param enumClass Enum class for which input must be a valid value
     * @param key Key for Result if input is not a valid value for enumClass
     * @param message Message for result if input is not a valid value for enumClass
     * @param code Code for Result if input is not a valid value for enumClass
     * @return Check to validate that input is a possible value for enumClass
     */
    Check isOneOf(Class<? extends Enum> enumClass,
                  String key,
                  String message = "should be one of ${EnumSet.allOf(enumClass)}",
                  String code = val.Result.CODE_ILLEGAL_VALUE) {
        satisfies({input ->
            if (!input || enumClass.isInstance(input)) return true
            try {
                Enum.valueOf(enumClass, input)
                true
            }
            catch (IllegalArgumentException ex) {
                false
            }
        }, key, message, code)
    }

    /**
     * Validate that input is not null
     * @param key Key under which Result will be added if input is null
     * @param message Message for Result if input is null
     * @param code Code for Result if input is null
     * @return Check to validate that input is not null
     */
    Check isNotNull(String key,
                    String message = "required field cannot be null",
                    String code = val.Result.CODE_REQUIRED_FIELD) {
        satisfies({input ->
            input != null }, key, message, code)
    }

    /**
     * Validate that input is null
     * This Check is not expected to be widely useful outside of things like
     * branching and may be replaced by something more general such as isEmpty
     * @param key Key under which Result will be added if input is not null
     * @param message Message for Result if input is not null
     * @param code Code for Result if input is not null
     * @return Check to validate that input is null
     */
    Check isNull(String key,
                 String message = "field must be null",
                 String code = val.Result.CODE_ILLEGAL_VALUE) {
        satisfies({input ->
            input == null }, key, message, code)
    }

    /**
     * Validate that the string representation of input matches the pattern specified (implicitly anchored)
     *
     * This will match using implicit stringifying (toString) so things like arrays
     * _could_ be tested (but probably shouldn't)
     * @param pattern String representation of regular expression to match
     * @param key Key for Result if input does not match regular expression
     * @param message Message for Result if input does not match regular expression
     * @param code Code for Result if input does not match regular expression
     * @return Check to validate that stringified input matches regular expression
     */
    Check matchesRe(String pattern,
                    String key,
                    String message = "does not match required pattern",
                    String code = val.Result.CODE_ILLEGAL_VALUE) {
        Pattern regex = ~pattern
        satisfies({input ->
            input == null || input.toString().matches(regex) }, key, message, code)
    }

    /**
     * Return a passing ResultMap.
     * @return Check that returns ResultMap.passed()
     */
    Check pass() {
        { input -> ResultMap.passed() }
    }

    //
    //Generalized building block type functions
    //
    /**
     * Invokes predicate function test with input and if the test returns false
     * returns a ResultMap containing an entry with a key of key and a Result having message and code
     * @param test Closure which accepts the input and returns a Boolean to determine whether to call onFail
     * @param key Key for Result if test(input) is false
     * @param message Message for Result if test(input) is false
     * @param code Code for Result if test(input) is false
     * @return Check to evaluate input
     */
    Check satisfies(Closure<Boolean> test,
                    String key,
                    String message,
                    String code) {
        satisfies(test, { input -> ResultMap.from([ (key): [new Result(message, code)] ]) })
    }

    /**
     * Invokes predicate function test passing input as an argument and returns onFail result if non-truthy
     * otherwise ResultMap.passed()
     * @param test Closure which returns a Boolean based on the value to determine whether to call onFail
     * @param onFail Closure which takes the input and returns the ResultMap which should indicate test failure
     * @return Check to evaluate input
     */
    Check satisfies(Closure<Boolean> test,
                    Closure<ResultMap> onFail) {
        { input ->
            test(input) ? ResultMap.passed() : onFail(input) }
    }

    //
    // COMBINATORS
    // Used to return a new check that is a combined version of the provided checks
    // All other functions should not expect collections of closures as client code can use the appropriate combinator
    //
    /**
     * Combine sequence of provided checks, execute all of them, and return merged results
     * @param rules Sequence of Checks, all of which wil be executed
     * @return Composed Check to evaluate input and return the merged ResultMap
     */
    Check all(Check... checks) {
        new AllCheck(checks as ArrayList)
    }

    /**
     * Combine sequence of provided checks with a short circuited logical `and`
     * If any check returns a non passing ResultMap, then stop iterating and return that ResultMap
     * @param checks Sequence of Checks to evaluate in order
     * @return Composed Check to evaluate input and return the first non-passing ResultMap else ResultMap.passed()
     */
    Check and(Check... checks) {
        new AndCheck(checks as ArrayList)
    }

    /**
     * Combine sequence of provided checks with a short circuited logical 'or'
     * If any check returns a passing ResultMap, than stop iterating and return that ResultMap
     * If no checks return a passing ResultMap, then return the last ResultMap
     * @param checks Sequence of Checks to evaluate in order
     * @return Composed Check to evaluate input and return the first ResultMap.passed() or last non-passing
     */
    Check or(Check... checks) {
        new OrCheck(checks as ArrayList)
    }

    //
    // OTHER HIGHER ORDER FUNCTIONS
    //
    /**
     * If testCheck(input) passes then evaluate bodyCheck(input) and return the result,
     * if testCheck(input) fails then return ResultMap.passed().
     * The results of testCheck are only used for the decision above and are not returned
     * @param testCheck If passing then evaluate bodyCheck
     * @param bodyCheck Evaluated and the results returned when testCheck passes
     * @return Check which will optionally evaluate input with bodyCheck
     */
    Check when(Check testCheck,
               Check bodyCheck) {
        { input ->
            testCheck(input) == ResultMap.passed() ? bodyCheck(input)
                                                   : ResultMap.passed() }
    }

    /**
     * If testCheck(input) passes then evaluate bodyCheck and return the result,
     * if testCheck(input) fails then return ResultMap.passed().
     * The results of testCheck are only used for the decision above and are not returned
     * @param testCheck If passing then evaluate bodyCheck
     * @param bodyCheck Evaluated and the results returned when testCheck passes
     * @return Check which will optionally evaluate input with bodyCheck
     */
    Check unless(Check testCheck,
                 Check bodyCheck) {
        { input ->
            testCheck(input) != ResultMap.passed() ? bodyCheck(input)
                                                   : ResultMap.passed() }
    }

    /**
     * Equivalent of a logical not but based on the ResultMap returned
     * Invert the Result by:
     * If check returns a ResultMap.passed() then return a
     * ResultMap constructed out of the arguments,
     * otherwise return a ResultMap.passed()
     *
     * This seems a little ugly and may be subject to change or removal.
     * A more reusable and less awkward approach may be something like (when(check, fail()))
     * This is presently oriented towards use in testChecks as part of when/unless/etc.
     * @param check Check for which the Result will be inverted
     * @param key Key for Result if inner Check originally passed
     * @param message Message for Result if inner Check passed
     * @param code Code for Result if inner Check passed
     * @return Check which will evaluate inner Check with input and return inverted Result
     */
    Check not(Check check,
              String key='not',
              String message='condition satisfied which should not have been',
              String code='NEGATED_CHECK') {
        { input ->
            check(input) == ResultMap.passed() ? ResultMap.from([(key): [new Result(message, code)]])
                                               : ResultMap.passed() }
    }

}
