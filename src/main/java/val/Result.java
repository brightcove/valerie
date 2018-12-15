package val;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Feedback for an individual validation rule.
 * An immutable value object representing one unit of feedback
 * resulting from the evaluation of a {@link Check} against provided input.
 * Will be aggregated into a {@link ResultMap}.
 * 
 * The feedback provided by this class is focused on a discrete
 * input value and the constraints affecting that value, while the
 * containing {@link ResultMap} contextualizes this information. In
 * other words this class doesn't indicate for which field the error
 * occurred or any similar path within the input graph, that
 * information is provided by the {@link ResultMap}.
 * 
 * The two fields (`code` and `message`)
 * provided by this class are intended to provide complementary
 * feedback:
 * 
 *   * `code` represents feedback which is
 * more readily acted upon by any consuming code. The standard use is to
 * indicate the _type_ of error out of a defined set of
 * possibilities; this would allow client code error handling to branch
 * based on the item without requiring additional parsing.
 *   * `message` provides any information needed
 * to resolve the specific issues encountered; this could include
 * information based on any actual parameters affecting evaluation.
 * 
 * With the above conventional uses, `code` could inform initial
 * dispatching of error handling code and `message` could
 * be used for more refined logic or to provide feedback to the user.
 * 
 * *The descriptions above and CODE constants provided below are
 * conventional only and do not have any logical significance:
 * replace or abandon at will.
 * Additionally "field" is used in the general sense throughout
 * rather than with the programming/Java connotations.*
 **/
public final class Result {

    /**
     * The value provided violates a defined constraint.
     **/
    public static final String CODE_ILLEGAL_VALUE = "ILLEGAL_VALUE";

    /**
     * The value provided is targetting a field which is not supported.
     **/
    public static final String CODE_ILLEGAL_FIELD = "ILLEGAL_FIELD";

    /**
     * A value has not been provided for a field which requires a value.
     **/
    public static final String CODE_REQUIRED_FIELD = "REQUIRED_FIELD";

    /**
     * The value provided is shorter than the minimum allowed length.
     **/
    public static final String CODE_TOO_SHORT = "TOO_SHORT";

    /**
     * The value provided is longer than the maximum allowed length.
     **/
    public static final String CODE_TOO_LONG = "TOO_LONG";

    private final String message;
    /**
     * Human readable information about this result.
     *
     * @return A string to be displayed to a user.
     **/
    public String getMessage() {
	return message;
    }

    private final String code;
    /**
     * Terse, machine-friendly information about this result.
     *
     * @return A string intended for the user or client logic.
     **/
    public String getCode() {
	return code;
    }

    /**
     * @deprecated Prefer #from
     * @private
     **/
    @EnsuresNonNull({"this.message", "this.code"})
    Result(String message, String code) {
	if (message == null)
	    throw new NullPointerException("Message must not be null");
	if (code == null) throw new NullPointerException("Code must not be null");
	this.message = message;
	this.code = code;
    }

    /**
     * Return a Result instance with the provided `message` and `code`.
     *
     * @param message Human readable information about this result.
     * @param code Terse, machine-friendly information about this result.
     * @return Result instance with provided values.
     * @throws NullPointerException if either property is null.
     **/
    @EnsuresNonNull({"#1", "#2"})
    public static Result from(String message, String code) {
	return new Result(message, code);
    }

    @Override
    public boolean equals(@Nullable Object other) {
	if (!(other instanceof Result)) return false;
	Result that = (Result) other;
	return Objects.equals(message, that.message) &&
	    Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
	return Objects.hash(message, code);
    }

    @Override
    public String toString() {
	return new StringBuilder(code)
	    .append(": ")
	    .append(message)
	    .toString();
    }
}
