package val

import groovy.transform.Immutable

/**
 * The information for a single result of evaluating a check to be aggregated in a {@link ResultMap}
 */
@Immutable
class Result {
    public static final String CODE_ILLEGAL_VALUE  = 'ILLEGAL_VALUE'
    public static final String CODE_ILLEGAL_FIELD  = 'ILLEGAL_FIELD'
    public static final String CODE_REQUIRED_FIELD = 'REQUIRED_FIELD'
    public static final String CODE_TOO_SHORT      = 'TOO_SHORT'
    public static final String CODE_TOO_LONG       = 'TOO_LONG'

    /**
     * Human readable information about this result
     */
    String message

    /**
     * Terser, machine-friendly information about this result
     */
    String code
}