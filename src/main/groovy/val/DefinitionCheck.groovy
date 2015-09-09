package val

/**
 * Represents a Check that is created through a "Definition": a closure using DSL style syntax to
 * define the validations that should be performed on an input graph.
 *
 * The initial instance will normally be created by the Idator and then further instances will be created
 * internally to form a graph of Checks which conceptually represents the structure of the input that will be validated
 * and onto which the input graph will map.
 *
 * DefinitionChecks will normally create a structure aligned with the expected input graph where each
 * DefinitionCheck will similarly be composed of those beneath it with a single DefinitionCheck attached to
 * the root of the input graph and containing all other DefinitionChecks (normally resulting in an arborescence)
 */
class DefinitionCheck implements Check {
    /**
     * A shared instance of Checkers which is used as a delegate for Definitions to allow Check construction
     * (and used internally)
     */
    @Delegate final Checkers checkers

    /**
     * A Map of stashed values shared among the graph of DefinitionChecks
     * allows access to stashed values within any defined checks
     */
    final Map stashed  //kind of like the Borg pattern

    /**
     * The default key for any ResultMaps from any Checks defined for this DefinitionCheck
     */
    String resultKey = 'error'

    //The check created by the Definition, target delegate for evaluation/call
    private Check mCheck

    //List of checks which are defined within the Definition
    private List definedChecks = new LinkedList()
    private List requiredChecks = new LinkedList()

    /**
     * Construct a new DefinitionCheck using provided arguments and definition
     * @param checkers Checkers to be used as delegate for Check construction
     * @param stashed Stash of values to be used across related DefinitionChecks
     * @param key Default key for ResultMap results from defined Checks
     * @param definition A Closure defining the checks for the input graph covered by this DefinitionCheck
     */
    public DefinitionCheck(Checkers checkers, Map stashed, String key, Closure definition){
        this.checkers = checkers
        this.stashed = stashed
        this.resultKey = key

        definition.delegate = this
        definition.resolveStrategy = Closure.DELEGATE_FIRST
        def definitionReturn = definition()
        if (definitionReturn instanceof Check) definedChecks << definitionReturn
        mCheck = new AndCheck(requiredChecks + [new AllCheck(definedChecks)])
    }

    /**
     * Evaluate input using the Check which has been defined in the Definition closure
     * @param input The input graph when this DefinitionCheck is at the root, otherwise the subgraph provided by
     * the parent DefinitionChecks
     * @return The ResultMap output for evaluating the input using this Check
     */
    @Override
    public ResultMap call(Object input) {
        if (toStash) stashed[toStash] = input  //Stash input if requested
        mCheck(input)
    }

    /**
     * Define a Check to be evaluated as part of this Definition
     * @param check Check which will be evaluated against input subgraph
     */
    void define(Check check) {
        definedChecks << check
    }

    /**
     * Define Checks for one or more children/successors nodes of the input graph.
     * Each key is the name of the child and each value will serve as a Definition for that child.
     *
     * This provides a natural way to define Checks against the children which can then be aggregated together into
     * a declarative Definition such as
     * <pre>
     * define id: { isNotNull() }
     * define name: {
     *     define first: { isNotNull() & isInstanceOf(String) },
     *             last: { isNotNull() & isInstanceOf(String) }
     * }
     * </pre>
     * This uses withValue internally and so will have similar behavior in regards to configuring the key
     * @param entries A Map of (child name):definition
     */
    void define(Map<String, Closure> entries) {
        entries.each{k,v->
            define(withValue(k, v))
        }
    }

    /**
     * Provides define style functionality but indicates that the children specified are tightly associated with
     * the present scope. Presently this results in the keys being qualified relative to the present scope
     * (.) separated.  As an example:
     * <pre>
     * define: name {
     *     subDefine first: { isNotNull() }
     * }
     * </pre>
     * would result in the key for the <q>first</q> check being <q>name.first</q> rather than just <q>first</q>
     * as it would have been with define.  subDefine can be thought of as extending the existing scope and
     * collecting results for children while define would be navigating/moving the root of the scope.
     * The two should be able to be nested and mixed appropriately
     *
     * @param entries a Map of (child name):definition
     */
    void subDefine(Map<String, Closure> entries) {
        entries.each{k,v->
            definedChecks << withSubValue(k, v)
        }
    }

    /**
     * Require a Check to pass before evaluating any `define`d Checks and return the result if not passing
     * This is analogous to establishing a precondition for the defined checks and should not be
     * regarded as a flexible flow control mechanism (which is available using the appropriate means of combination)
     *
     * Multiple required Checks will be evaluated in sequence (using short-circuiting AND) before any defined Checks.
     * @param check Check which will be evaluated against input graph before `define`d Checks
     */
    void require(Check check) {
        requiredChecks << check
    }

    //Under which key the value for this scope should be stashed if any
    private String toStash
    /**
     * Stash the value from the input graph during evaluation phase so it could be used by other Checks
     * Invoking stashValueAs 'foo' will allow any other Check within the same Definition graph to access
     * that value as stashed.foo
     * @param name The key under which this value will be stashed
     */
    void stashValueAs(String name) {
        toStash = name
    }

    /**
     * Processes a Definition with the same key and graph as the parent Definition. Useful for organization.
     * @param definition Definition from which to create Check
     * @return Check created from provided Definition
     */
    Check withValue(Closure definition){
        withValue(null, resultKey, definition)
    }

    /**
     * Process a Definition using the named child node as the root of the input subgraph and the child name as the key
     * @param child Name of child node in graph which will serve as the root of the subgraph passed to the Definition
     * if null use existing root and graph
     * @param definition Definition to create Check for graph/subgraph
     * @return Check created from thee provided Definition
     */
    Check withValue(String child, Closure definition) {
        withValue(child, child, definition)
    }

    /**
     * Process a Definition using the named child node as the root of the input subgraph and the provided key as the
     * default key
     * @param child Name of child node in graph which will serve as the root of the subgraph passed to the Definition
     * if null use existing root and graph
     * @param resultKey Key to use by default for entries in ResultMaps returned by Checks
     * @param definition Definition to create Check for graph/subgraph
     * @return Check created from the provided Definition
     */
    Check withValue(String child, String resultKey, Closure definition) {
        def scope = new DefinitionCheck(checkers, stashed, resultKey, definition)
        return { input ->
            def value = input == null ? null            // if input is null return null
                      : child == null ? input           // if child is null use present object
                                      : input[child]    // otherwise descend to subgraph
            scope(value) }
    }

    /**
     * Process a Definition using the named child node as the root of the input subgraph and
     * adding the child name to the present key (`.` separated) to create the new default key
     * Used by and conceptually identical to subDefine
     * @param child Name of child node in graph which will serve as the root of the subgraph passed to the Definition
     * if null use existing root and graph
     * @param definition Definition to create Check for graph/subgraph
     * @return Check created from thee provided Definition
     */
    Check withSubValue(String child, Closure definition) {
        withValue(child, "${this.resultKey}.${child}", definition)
    }

    /**
     * Process a Definition for each of the items in the Iterable within the input graph and return the merged
     * ResultMap
     * Uses Groovy duckiness so some things that aren't strictly Iterables will work (i.e. Maps)
     * @param definition Definition to a create Check which will evaluate each item from the Iterable
     * @return Check which will iterate over the Iterable and evaluate the defined Check for each item
     */
    Check withEachValue(Closure<Check> definition) {
        def scope = new DefinitionCheck(checkers, stashed, resultKey, definition)
        return { input ->
            def results = ResultMap.passed()
            //Evaluate each input value using the same Check
            input.each{
                results += scope(it) }
            results }
    }

    /**
     * Define a Map where where the Definition (value) for the first matching condition (key
     * will be evaluated.
     *
     * Since the keys are likely to be fairly complex expressions Groovy syntactical shortcuts are not
     * likely to work consistently.
     *
     * otherwise() is provided to allow configuration of a default clause.
     *
     * @param mapping Map where each key is a Check which will determine whether this clause will be evaluated
     * and each value is the Definition to evaluate if the condition passes
     * @return Check which will evaluate the first Definition with a passing condition,
     * if none match returns ResultMap.passed()
     */
    Check cond(LinkedHashMap<Check, Closure> mapping) {
        LinkedHashMap<Check, Check> condMap = mapping.collectEntries {
            [(it.key): new DefinitionCheck(checkers, stashed, resultKey, it.value)]
        }
        return { input ->
            for (Map.Entry<Check, Check> entry: condMap) {
                if ((entry.key.call(input)) == ResultMap.passed()) {
                    return entry.value.call(input)
                }
            }
            ResultMap.passed()
        }
    }

    // All of these calls delegate to Checkers
    // In some future version the internals of this should possibly be shuffled around to allow easier extensibility
    // and separation of concerns. The idea would be to create a designated mediator that handles the resultKey
    // behavior while also providing a consolidated container for the checks available.
    // This is unjustifiably complex at the moment but could be helpful if this becomes a generalized library
    // A basic design would be to replace the Checkers reference with the mediator, modify each method in the mediator
    // to expect a scope instance (provider of resultKey), and then modify the metaClass for this behavior to delegate
    // to the mediator passing the present instance. This would allow easy registration of per val.Idator custom checks.
    // TODO: Extract this comment into a more proper home when the project also has a home
    /**
     * {@link val.Checkers#when} accepting a Definition for bodyCheck
     */
    Check when(Check check, Closure definition) {
        checkers.when(check, new DefinitionCheck(checkers, stashed, resultKey, definition))
    }
    /**
     * {@link val.Checkers#unless} accepting a Definition for bodyCheck
     */
    Check unless(Check check, Closure closure) {
        checkers.unless(check, new DefinitionCheck(checkers, stashed, resultKey, closure))
    }

    /**
     * {@link val.Checkers#hasMember} passing the key for this instance
     */
    Check hasMember(String field) {
        checkers.hasMember(field, resultKey) }
    /**
     * {@link val.Checkers#hasOnlyFieldsIn} passing the key for this instance
     */
    Check hasOnlyFieldsIn(Class targetClass) {
        checkers.hasOnlyFieldsIn(targetClass, resultKey)
    }
    /**
     * (@link val.Checkers#hasSizeGte} passing the key for this instance
     */
    Check hasSizeGte(Integer min) {
        checkers.hasSizeGte(min, resultKey) }
    /**
     * {@link val.Checkers#hasSizeLte} passing the key for this instance
     */
    Check hasSizeLte(Integer max) {
        checkers.hasSizeLte(max, resultKey) }
    /**
     * {@link val.Checkers#hasValueGte} passing the key for this instance
     */
    Check hasValueGte(Integer max) {
        checkers.hasValueGte(max, resultKey) }
    /**
     * {@link val.Checkers#hasValueLte} passing the key for this instance
     */
    Check hasValueLte(Integer max) {
        checkers.hasValueLte(max, resultKey) }
    /**
     * {@link val.Checkers#isNotNull} passing the key for this instance
     */
    Check isNotNull() {
        checkers.isNotNull(resultKey) }
    /**
     * {@link val.Checkers#isNull} passing the key for this instance
     */
    Check isNull() {
        checkers.isNull(resultKey) }
    /**
     * {@link val.Checkers#isOneOf} passing the key for this instance
     */
    Check isOneOf(Class<? extends Enum> type) {
        checkers.isOneOf(type, resultKey) }
    /**
     * {@link val.Checkers#isInstanceOf} passing the key for this instance
     */
    Check isInstanceOf(Class<?> type) {
        checkers.isInstanceOf(type, resultKey) }
    /**
     * {@link val.Checkers#matchesRe} passing the key for this instance
     */
    Check matchesRe(String pattern) {
        checkers.matchesRe(pattern, resultKey) }
    /**
     * Alias for {@link val.Checkers#pass} to provide a more readable default clause in a {@link #cond}
     */
    Check otherwise() {
        checkers.pass() }
}