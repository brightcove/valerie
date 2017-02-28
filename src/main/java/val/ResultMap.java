package val;

import java.util.*;

/**
 * A collection of all {@link Result}s returned by evaluating a Checkers
 * This is effectively a Map where the key is an identifier for the result
 * (normally the field being checked)
 * and the value is the list of all of the {@link Result}s for that key.
 *
 * If the map is empty then the checks have nothing to say (everything passed).
 * ResultMaps are immutable value objects
 */
public class ResultMap {
    private final Map<String, List<Result>> values;

    //The vast majority/all of the values will normally be equal to this, so a
    // flyweight is used
    //This behavior should not be considered part of the exposed API or relied
    // on in any client code
    final static ResultMap PASSED = new ResultMap(new HashMap<>());

    /**
     * A convenience factory method for an empty/passing ResultMap
     * @return ResultMap with no entries
     */
    public static ResultMap passed() {
        return PASSED;
    }

    /**
     * Retrieve a ResultMap containing the entries in the provided Map.
     * This is the standard way of retrieving a ResultMap
     * @param map A Map with entries where the keys are groupings
     * and the values are the list of Results
     * @return A ResultMap with the entries provided
     */
    public static ResultMap from(Map<String, List<Result>> map) {
        if (map.isEmpty()) return PASSED;
        return new ResultMap(map);
    }

    public static ResultMap from(String key, List<Result> results) {
        Map<String, List<Result>> map = new HashMap<>();
        map.put(key, results);
        return ResultMap.from(map);
    }

    //Hide the default constructor to allow avoiding instantiation and allow
    // flyweight style optimizations
    private ResultMap(Map<String, List<Result>> pValues) {
        this.values = new HashMap<String, List<Result>>(pValues);
    }

    /**
     * Return a representation of this ResultMap as a Map
     * @return A Map containing the entries within this ResultMap
     */
    public Map<String, List<Result>> asMap() {
        return new HashMap<>(values);
    }

    /**
     * Return a ResultMap which contains the merged result of the entries of
     * this ResultMap and the one passed as an argument
     * @param right The ResultMap to be merged with this ResultMap
     * @return a ResultMap containing the merged entries of this and right
     */
    ResultMap plus(ResultMap right) {
        if (right.values.isEmpty()) return this;
        if (this.values.isEmpty()) return right;
        Map<String, List<Result>> merged = new HashMap<>(this.values);
        for (Map.Entry<String, List<Result>> e: right.values.entrySet()) {
            String k = e.getKey();
            if (merged.containsKey(k)) {
                merged.get(k).addAll(e.getValue());
            } else {
                merged.put(k, e.getValue());
            }
        }
        return new ResultMap(merged);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ResultMap &&
            Objects.equals(values, ((ResultMap) other).values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder("ResultMap:")
            .append(values.toString())
            .toString();
    }
}
