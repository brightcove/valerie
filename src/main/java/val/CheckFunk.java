package val;

public interface CheckFunk<T> {
    T call(Object input, EvalContext ctx);
}
