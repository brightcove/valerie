package val

/**
 * Serves as the entry point to validating an object graph. This is effectively a factory for the
 * initial DefinitionCheck which can begin validation.
 */
class Idator {

    /**
     * Start defining Checks at the root of the input graph
     * @param definition A Closure which will be evaluated against the root DefinitionCheck of the input graph
     * @return The Check created by definition
     */
    Check define(Closure definition) {
        new DefinitionCheck(new val.Checkers(), [:], 'root', definition)
    }

    /**
     * Register the provided closure as a Checker to be able to be used within the definitions
     * @param name The name by which the Checker can be called
     * @param closure Closure containing logic to create Check
     */
    //TODO: allow per instance extension
    void registerChecker(String name, Closure closure) {
        DefinitionCheck.metaClass[name] = closure
    }
}