package val;

import org.checkerframework.checker.nullness.qual.*;

//TODO: 0.5.0-Hopefull this will be designed out, otherwise it should be documented
public class NopTransformer extends TransformerCheck {
    @EnsuresNonNull({"#1"})
    public NopTransformer(Check nestedCheck) {
        super(nestedCheck);
    }

    @Override
    protected @Nullable Object transform(@Nullable Object input) { return input; }
}
