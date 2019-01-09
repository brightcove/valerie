package val

abstract class TransformerCheck extends Check {
    Check nestedCheck
    String toStash

    def transform(Object input) { input }

    @Override
    ResultMap call(Object input, EvalContext ctx) {
        def transformedInput = transform(input)
        if (toStash) ctx.stashed."${toStash}" = transformedInput
        nestedCheck(transformedInput, ctx)
    }
}

class NopTransformer extends TransformerCheck {}

class ChildTraverser extends TransformerCheck {
    String childName

    @Override
    def transform(Object input) {
        if (input == null) return null;
        if (childName == null) return input;
        input[childName]
    }
}
