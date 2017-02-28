package val

import val.Result

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
class Checkers extends CheckFactory {

    //Hack to provide completion
    @Delegate()
    Idator dummyMethods = new Idator()

    /**
     * Validate that the input contains `member` (regardless of value)
     * @param member The name of the member that input should contain
     * @param mold Mold for ResultMap if member does not exist
     * @return Check to validate that input contains specified member
     */
    Check hasMember(Map mold=[:], String member) {
        mold = [msg:"required field ${member} is not present",
                code:Result.CODE_REQUIRED_FIELD] + mold
        satisfies(mold) { input, ctx -> input?.containsKey(member) }
    }

    /**
     * Validates that map only contains members that are in fields
     * @param fields Set of field names allowed in input
     * @param mold Mold for ResultMap for input members not in fields
     * key will serve as a prefix which will have .${input field name} appended
     * if key is not provided then only the input field name will be used
     * @return Check which will validate field membership
     */
    Check hasOnlyFieldsIn(Map mold=[:], Set<String> fields) {
        mold = [msg:'field is unknown',
                code:Result.CODE_ILLEGAL_FIELD] + mold
        { input, ctx ->
            if (!(input instanceof Map))
                return ResultMap.from(
                    (mold.key): [new Result('only maps are supported',
                                       'ILLEGAL_VALUE')])
            Map collectedResults = [:]
            ((Map) input).keySet().each{ fieldName ->
                if (!fields.contains(fieldName)) {
                    String prefix = mold.key ? "${mold.key}." : ''
                    collectedResults."${prefix}${fieldName}" =
                        [new Result(mold.msg, mold.code)]
                }
            }
            ResultMap.from(collectedResults)
        }
    }

    /**
     * Validates that map only contains fields declared in targetClass
     * @param targetClass Class whose fields will become the Set of valid fields
     * @param mold Mold for ResultMap for input members not in fields
     * key will serve as a prefix which will have .${input field name} appended
     * if key is not provided then only the input field name will be used
     * @return Check which will validate field membership
     */
    Check hasOnlyFieldsIn(Map mold=[:], Class targetClass) {
        mold = [msg:'field is unknown',
                code:Result.CODE_ILLEGAL_FIELD] + mold
        hasOnlyFieldsIn(mold,
                        targetClass.getDeclaredFields()*.name as Set)
    }

    /**
     * Validate that the size of input is >= (gte) the min size.
     * Uses Groovy size duckiness
     * @param min Minimum (inclusive) size for input
     * @param mold Mold for ResultMap when size is less than min
     * @return Check to validate input has size greater than or equal to min
     */
    Check hasSizeGte(Map mold=[:], Integer min) {
        mold = [msg: "should be at least ${min} long",
                code: Result.CODE_TOO_SHORT] + mold
        satisfies(mold) { input, ctx ->
            input == null || input?.size() >= min
        }
    }

    /**
     * Validate that the size of input is <= (lte) the max size.
     * Uses Groovy size duckiness
     * @param max Maximum (inclusive) size for input
     * @param mold Mold for ResultMap if value is input than max
     * @return Check to validate that input has a size less than or equal to max
     */
    Check hasSizeLte(Map mold=[:], Integer max) {
        mold = [msg:"should be no longer than ${max}",
                code:Result.CODE_TOO_LONG] + mold
        satisfies(mold) { input, ctx -> input?.size() <= max }
    }

    /**
     * Validate that the value of input is >= (gte) min
     * @param min Minimum (inclusive) value for input
     * @param mold Mold for ResultMap if input is less than min
     * @return Check to validate input has value greater than or equal to min
     */
    Check hasValueGte(Map mold=[:], Object min) {
        mold = [msg:"should not be less than ${min}",
                code:Result.CODE_ILLEGAL_VALUE] + mold
        satisfies(mold) { input, ctx ->
            input == null || input >= min
        }
    }

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
        satisfies(mold) { input, ctx ->
            input != null }
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
            testCheck(input, ctx) == ResultMap.passed() ? bodyCheck(input, ctx)
                                                        : ResultMap.passed() }
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
            testCheck(input, ctx) != ResultMap.passed() ? bodyCheck(input, ctx)
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
            check(input, ctx) == ResultMap.passed() ? ResultMap.from(
              [(mold.key.toString()): [new Result(mold.msg.toString(),
                                                  mold.code.toString())]])
                                               : ResultMap.passed() }
    }

}
