package val;

abstract class TransformerCheck extends Check {
    private Check nestedCheck;
    private String toStash;

    public void setNestedCheck(Check nestedCheck) {
        this.nestedCheck = nestedCheck;
    }
    public void setToStash(String toStash) {
        this.toStash = toStash;
    }

    public Object transform(Object input) {
        return input;
    }

    @Override
    public ResultMap call(Object input, EvalContext ctx) {
        Object transformedInput = transform(input);
        if (toStash != null) ctx.getStashed().put(toStash, transformedInput);
        return nestedCheck.call(transformedInput, ctx);
    }
}
