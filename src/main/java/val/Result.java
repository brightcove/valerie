package val;

import java.util.Objects;

/**
 * A single Check result to be aggregated in a {@link ResultMap}
 */
public class Result {
    public static final String CODE_ILLEGAL_VALUE  = "ILLEGAL_VALUE";
    public static final String CODE_ILLEGAL_FIELD  = "ILLEGAL_FIELD";
    public static final String CODE_REQUIRED_FIELD = "REQUIRED_FIELD";
    public static final String CODE_TOO_SHORT      = "TOO_SHORT";
    public static final String CODE_TOO_LONG       = "TOO_LONG";

    /**
     * Human readable information about this result
     */
    private final String msg;
    public String getMsg() {
        return msg;
    }

    /**
     * Terser, machine-friendly information about this result
     */
    private final String code;
    public String getCode() {
        return code;
    }

    public Result(String msg, String code) {
        this.msg = msg;
        this.code = code;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Result)) return false;
        Result that = (Result) other;
        return Objects.equals(msg, that.msg) &&
            Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(msg, code);
    }

    @Override
    public String toString() {
        return new StringBuilder(code)
            .append(": ")
            .append(msg)
            .toString();
    }
}
