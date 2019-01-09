package val

/**
 * Represents a Check that is created through a "Definition": a closure using
 * DSL style syntax to
 * define the validations that should be performed on an input graph.
 *
 * The initial instance will normally be created by the Idator and then further
 * instances will be created
 * internally to form a graph of Checks which conceptually represents the
 * structure of the input that will be validated
 * and onto which the input graph will map.
 *
 * Idators will normally create a structure aligned with the expected input
 * graph where each
 * Idator will similarly be composed of those beneath it with a single
 * Idator attached to
 * the root of the input graph and containing all other Idators
 * (normally resulting in an arborescence)
 */
class Idator<T extends Checkers> extends Check {

    /**
     * A shared instance of Checkers which is used as a delegate for
     * Idators to allow Check construction
     * (and used internally)
     */
    T checkers

    /**
     * The default key for any ResultMaps from Checks defined in this Idator
     */
    String resultKey = 'error'

    //The check created by the Definition, target delegate for evaluation/call
    private TransformerCheck mCheck

    //List of checks which are defined within the Definition
    private List definedChecks = new LinkedList()
    private List requiredChecks = new LinkedList()

    /**
     * Construct a new Idator using provided arguments and definition
     * @param key Default key for ResultMap results from defined Checks
     */
    Idator(String key = 'root',
           TransformerCheck mCheck = new NopTransformer()){
        this.resultKey = key
        this.mCheck = mCheck
    }

    // Runtime metaprogramming to dispatch to Checkers

    // Will presently most likely have issues if Maps are non-mold parameters
    // This could be avoided through some form of counting or an annotation
    // The solution is deferred until the problem arises

    /**
     * Create an invocation of the provided function with
     * a mold that represents the present context, either by creating
     * a new argument for that mold or modifying a previously provided one.
     */
    def addContextToMold = { name ->
        { Object[] args ->
            // if no args were passed just call with a mold based on the context
            if (!args) {
                checkers."${name}"(key:delegate.resultKey)
            }
            // if the first arg is a map, assume it is the mold
            else if (args[0] instanceof Map)  {
                args[0] = [key:delegate.resultKey] + args[0]
                checkers."${name}"(*args)
            }
            // else the optional mold was not provided so add one as first arg
            else {
                checkers."${name}"([key:delegate.resultKey], *args)
            }
        }
    }

    def methodMissing(String name, args) {
        def types = args.collect{it.getClass()}
        def check

        if (moldCouldBeAdded(name, types) ){
            check = addContextToMold(name)
        }
        else if (providedCallIsValid(name, types)) {
	    //Contextualize existing mold with defaults if present
            if (moldIsPresent(args)) check = addContextToMold(name)
            else check = checkers.&"${name}"
        }
        else throw new MissingMethodException(name, this.class, args)
        // Shorten future dispatching
        this.metaClass."${name}" = check
        check(*args)
    }

    /**
     * Did the provided call omit the mold?
     */
    private Boolean moldCouldBeAdded(String name, List<Class<?>> types) {
      checkers?.metaClass?.respondsTo(checkers, name, Map, *types)
    }

    /**
     * Is the provided call a valid signature for Checkers?
     */
    private Boolean providedCallIsValid(String name, List<Class<?>> types) {
      checkers?.metaClass?.respondsTo(checkers, name, *types)
    }

    /**
     * Does the argument list include an object which is likely a mold?
     */
    private Boolean moldIsPresent(args) {
      args && args[0] instanceof Map
    }

    def childIdator(LinkedHashMap overrides = [:]) {
        //Create array for positional parameters merging in overrides
        def args = [overrides.resultKey   ?: resultKey]
        if (overrides['mCheck']) args << overrides['mCheck']
        this.class.newInstance(*args)
    }

    def using(@DelegatesTo.Target T checkers,
              @DelegatesTo(strategy = Closure.DELEGATE_FIRST)
                      Closure definition) {
        this.checkers = checkers
        definition.delegate = this
        definition.resolveStrategy = Closure.DELEGATE_FIRST
        def definitionReturn = definition()
        if (definitionReturn instanceof Check) definedChecks << definitionReturn
        mCheck.nestedCheck =
                new AndCheck(requiredChecks + [new AllCheck(definedChecks)])
        this
    }

    /**
     * Evaluate input using the Check which has been defined in the Definition
     * closure
     * @param input The input graph when this Idators is at the root,
     * otherwise the subgraph provided by
     * the parent Idators
     * @return The ResultMap output for evaluating the input using this Check
     */
    @Override
    ResultMap call(Object input, EvalContext ctx) {
        mCheck(input, ctx)
    }

    /**
     * Define a Check to be evaluated as part of this Definition
     * @param check Check which will be evaluated against input subgraph
     */
    void define(Check check) {
        definedChecks << check
    }

    /**
     * Define Checks for one or more input graph children/successors nodes
     * Each key is the child name and each value is the respective Definition
     *
     * This provides a natural way to define Checks against the children which
     * can then be aggregated together into a declarative Definition such as.
     * <pre>
     * define id: { isNotNull() }
     * define name: {
     *     define first: { isNotNull() & isInstanceOf(String) },
     *             last: { isNotNull() & isInstanceOf(String) }
     * }
     * </pre>
     * This uses withValue internally.
     * @param entries A Map of (child name):definition
     */
    void define(Map<String, Closure> entries) {
        entries.each{k,v->
            define(withValue(k, v))
        }
    }

    /**
     * Provides define style functionality but indicates that the children
     * specified are tightly associated with
     * the present scope. Presently this results in the keys being qualified
     * relative to the present scope
     * (.) separated.  As an example:
     * <pre>
     * define: name {
     *     subDefine first: { isNotNull() }
     * }
     * </pre>
     * would result in the key for the <q>first</q> check being
     * <q>name.first</q> rather than just <q>first</q>
     * as it would have been with define.  subDefine can be thought of as
     * extending the existing scope and
     * collecting results for children while define would be navigating/moving
     * the root of the scope.
     * The two should be able to be nested and mixed appropriately
     *
     * @param entries a Map of (child name):definition
     */
    void subDefine(Map<String, Closure> entries) {
        entries.each{k,v->
            definedChecks << withSubValue(k, v)
        }
    }
    void has(Map<String, Closure> entries) {
        subDefine(entries)
    }

    /**
     * Require a Check to pass before evaluating any `define`d Checks and return
     * the result if not passing.
     * This is analogous to establishing a precondition for the defined checks
     * and should not be
     * regarded as a flexible flow control mechanism (which is available using
     * the appropriate means of combination)
     *
     * Multiple required Checks will be evaluated in sequence (using
     * short-circuiting AND) before any defined Checks.
     * @param check Check which will be evaluated against input graph before
     * `define`d Checks
     */
    void require(Check check) {
        requiredChecks << check
    }

    /**
     * require analog for define Map syntax.
     * See require(Check) and define(Map) for further information
     */
    void require(Map<String, Closure> entries) {
        entries.each{k,v->
            require(withValue(k, v))
        }
    }

    /**
     * Stash the value from the input graph during evaluation phase so it could
     * be used by other Checks
     * Invoking stashValueAs 'foo' will allow any other Check within the same
     * Definition graph to access
     * that value as stashed.foo
     * @param name The key under which this value will be stashed
     */
    void stashValueAs(String name) {
        mCheck.toStash = name
    }

    /**
     * Processes a Definition with the same key and graph as the parent
     * Definition. Useful for organization.
     * @param definition Definition from which to create Check
     * @return Check created from provided Definition
     */
    @Deprecated
    Check withValue(Closure definition){
        valueOf(null, resultKey, definition)
    }
    Check valueOf(Closure definition) {
        valueOf(null, resultKey, definition)
    }

    /**
     * Process a Definition using the named child node as the root of the input
     * subgraph and the child name as the key
     * @param child Name of child node in graph which will serve as the root of
     * the subgraph passed to the Definition
     * if null use existing root and graph
     * @param definition Definition to create Check for graph/subgraph
     * @return Check created from thee provided Definition
     */
    @Deprecated
    Check withValue(String child, Closure definition) {
        withValue(child, child, definition)
    }
    Check valueOf(String child, Closure definition) {
        valueOf(child, child, definition)
    }

    /**
     * Process a Definition using the named child node as the root of the input
     * subgraph and the provided key as the
     * default key
     * @param child Name of child node in graph which will serve as the root of
     * the subgraph passed to the Definition
     * if null use existing root and graph
     * @param resultKey Key to use by default for entries in ResultMaps
     * returned by Checks
     * @param definition Definition to create Check for graph/subgraph
     * @return Check created from the provided Definition
     */
    @Deprecated
    Check withValue(String child, String resultKey, Closure definition) {
        valueOf(child, resultKey, definition)
    }
    Check valueOf(String child, String resultKey, Closure definition) {
        childIdator(resultKey: resultKey,
                mCheck: new ChildTraverser(childName: child)).using(
                checkers, definition)
    }
    /**
     * Process a Definition using the named child node as the root of the input
     * subgraph and
     * adding the child name to the present key (`.` separated) to create the
     * new default key
     * Used by and conceptually identical to subDefine
     * @param child Name of child node in graph which will serve as the root
     * of the subgraph passed to the Definition
     * if null use existing root and graph
     * @param definition Definition to create Check for graph/subgraph
     * @return Check created from thee provided Definition
     */
    Check withSubValue(String child, Closure definition) {
        valueOf(child, "${this.resultKey}.${child}", definition)
    }

    /**
     * Process a Definition for each of the items in the Iterable within the
     * input graph and return the merged
     * ResultMap
     * Uses Groovy duckiness so some things that aren't strictly Iterables will
     * work (i.e. Maps)
     * @param definition Definition to a create Check which will evaluate each
     * item from the Iterable
     * @return Check which will iterate over the Iterable and evaluate the
     * defined Check for each item
     */
    Check withEachValue(Closure<Check> definition) {
        def scope = childIdator().using(checkers, definition)
        return { input, ctx ->
            def results = ResultMap.CLEAN
            //Evaluate each input value using the same Check
            input.each{ results += scope(it, ctx) }
            results
	}
    }

    //FIXME: This belongs in Checkers
    /**
     * Define a Map where where the Definition (value) for the first matching
     * condition (key will be evaluated).
     *
     * Since the keys are likely to be fairly complex expressions Groovy
     * syntactical shortcuts are not likely to work consistently.
     * otherwise() is provided to allow configuration of a default clause.
     *
     * @param mapping Map where each key is a Check which will determine
     * whether this clause will be evaluated
     * and each value is the Definition to evaluate if the condition passes
     * @return Check which will evaluate the first Definition with a passing
     * condition,
     * if none match returns ResultMap.passed()
     */
    Check cond(LinkedHashMap<Check, Closure> mapping) {
        cond({input->input}, mapping)
    }
    Check cond(Closure accessor, LinkedHashMap<Check, Closure> mapping) {
        LinkedHashMap<Check, Check> condMap = mapping.collectEntries {
            [(it.key): childIdator().using(checkers, it.value)]
        }
        return { input, ctx ->
            for (Map.Entry<Check, Check> entry: condMap) {
                if ((entry.key.call(accessor(input), ctx)) == ResultMap.CLEAN) {
                    return entry.value.call(input, ctx)
                }
            }
            ResultMap.CLEAN
        }
    }

    /**
     * {@link val.Checkers#when} accepting a Definition for bodyCheck
     */
    Check when(Check check, Closure definition) {
        checkers.when(check, childIdator().using(checkers, definition))
    }
    /**
     * {@link val.Checkers#unless} accepting a Definition for bodyCheck
     */
    Check unless(Check check, Closure closure) {
        checkers.unless(check, childIdator().using(checkers, closure))
    }
}
