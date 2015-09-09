package val

/**
 * A collection of all {@link Result}s returned by evaluating a Checkers
 * This is effectively a Map where the key is an identifier for the result (normally the field being checked)
 * and the value is the list of all of the {@link Result}s for that key.
 *
 * If the map is empty then the checks have nothing to say (everything passed).
 * ResultMaps are immutable value objects
 */
class ResultMap {
    private final Map<String, List<Result>> values

    //The vast majority/all of the values will normally be equal to this, so a flyweight is used
    //This behavior should not be considered part of the exposed API or relied on in any client code
    final static ResultMap PASSED = new ResultMap([:])

    /**
     * A convenience factory method for an empty/passing ResultMap
     * @return ResultMap with no entries
     */
    public static ResultMap passed() {
        return PASSED
    }

    /**
     * Retrieve a ResultMap containing the entries in the provided Map.
     * This is the standard way of retrieving a ResultMap
     * @param map A Map with entries where the keys are groupings (normally fields)
     * and the values are the list of Results
     * @return A ResultMap with the entries provided
     */
    public static ResultMap from(Map<String, Iterable<Result>> map) {
        if (map.size() == 0) return PASSED
        new ResultMap(map)
    }

    //Hide the default constructor to allow avoiding instantiation and allow flyweight style optimizations
    private ResultMap(Map<String, List<Result>> pValues) {
        this.values = new HashMap<>(pValues)
    }

    /**
     * Return a representation of this ResultMap as a Map
     * @return A Map containing the entries within this ResultMap
     */
    public Map<String, List<Result>> asMap() {
        return new HashMap<>(values)
    }

    /**
     * Return a ResultMap which contains the merged result of the entries of
     * this ResultMap and the one passed as an argument
     * @param right The ResultMap whose entries will be merged with those of this ResultMap
     * @return a ResultMap containing the merged entries of this and right
     */
    ResultMap plus(ResultMap right) {
        if (right.values.size() == 0) return this
        if (this.values.size() == 0) return right
        Map merged = new HashMap<>(this.values)
        right.values.each{ k, v ->
            merged[k] = merged.containsKey(k) ? merged[k] + v : v
        }
        return new ResultMap(merged)
    }

    //Groovy magic wasn't working consistently so these methods are created explicitly
    @Override
    boolean equals(Object obj) {
        return obj instanceof ResultMap && this.values == obj.values
    }

    @Override
    int hashCode() {
        return values.hashCode()
    }
}