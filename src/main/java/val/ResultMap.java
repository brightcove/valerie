package val;

import java.util.*;

import org.checkerframework.checker.nullness.qual.*;

/**
 * An immutable value object which conveys
 * the result of an evaluation of a {@link Check} against a provided input.
 * This is represented as a Map where the {@code key} defines the context
 * for specific feedback and the value is a {@link Result} object
 * prooviding that feedback. In conventional use the key will be the
 * field or complete path to a field for which the {@link Result}
 * applies.
 * <p>
 * If the map is empty then the checks have nothing to say (everything passed)
 * and the ResultMap is considered {@code clean}.
 * </p><p>
 * ResultMaps are immutable and intended to be combined using the {@link #plus}
 * method. This adheres to functional practices (monad-y containing of side
 * effects), defines a clear relationship with combining
 * {@link Check}s and facilitates concurrency.
 * </p>
 */
public class ResultMap {

    /**
     * The results this ResultMap represents, where the keys identify
     * the path or similar context and the values are the list of
     * {@link Result}s for that context.
     */
    private final Map<String, List<Result>> values;

    /**
     * Whether the evaluation which created this ResultMap warranted no
     * feedback.
     */
    private final boolean clean;

    /**
     * Whether the evaluation which created this ResultMap warranted no
     * feedback.
     * Stored eagerly, primarily to optimize for {@link #CLEAN} flyweight.
     * @return true if the ResultMap contains no feedback, false otherwise
     */
    public boolean isClean() { return clean; }

    /**
     * An empty ResultMap instance which should be used in cases where there is
     * no feedback to provide.
     * This instance is used as a flyweight as it is expected to be the result
     * for the majority of evaluations (but the flyweight identity should not
     * be relied upon by any client code).
     */
    final static ResultMap CLEAN = new ResultMap(new HashMap<>());

    /**
     * Create a ResultMap containing the entries in the provided Map.
     * This is the standard way of creating a ResultMap
     *
     * @param map A Map with entries where the keys are context
     * and the values are the list of Results
     * @return A ResultMap with the entries provided
     */
    @EnsuresNonNull({"#1"})
    public static ResultMap from(Map<String, List<Result>> map) {
        if (map == null) throw new NullPointerException("map must not be null");
        if (map.isEmpty()) return CLEAN;
        //TODO: Ensure map does not contain any nulls
        return new ResultMap(map);
    }

    /**
     * Creates a ResultMap with an entry where the key is
     * {@code key} an the value is {@code results}.
     *
     * @param key The String key for the entry in the new object
     * @param results The list of {@link Result}s to associate with
     * key in the new object.
     * @return A ResultMap containing the provided entry
     * @throws {@link NullPointerException} if either argument is null.
     */
    public static ResultMap from(String key, List<Result> results) {
        if (key == null) throw new NullPointerException("key must not be null");
        if (results == null) throw new NullPointerException("results must not be null");
        Map<String, List<Result>> map = new HashMap<>();
        map.put(key, results);
        return ResultMap.from(map);
    }

    /**
     * Internal constructor which ensures immutability of contained content.
     */
    private ResultMap(@NonNull Map<String, List<Result>> pValues) {
        Map<String, List<Result>> values = new HashMap<String, List<Result>>(pValues.size());
        for (Map.Entry<String, List<Result>> entry: pValues.entrySet()) {
            if (entry.getKey() == null) throw new NullPointerException("All keys must not be null");
            List<Result> from = entry.getValue();
            if (from == null) throw new NullPointerException("All values must not be null");
            List<Result> results = new ArrayList<Result>(from.size());
            for (Result result: from) {
                if (result == null) throw new NullPointerException("All Results must not be null");
                results.add(result);
            }
            values.put(entry.getKey(), results);
        }
        this.values = values;
        clean = this.values.isEmpty();
    }

    /**
     * Return a representation of this ResultMap as a Map.
     * @return A Map containing the entries within this ResultMap.
     */
    public Map<String, List<Result>> asMap() {
        Map<String, List<Result>> valueCopy = new HashMap<>();
        for (Map.Entry<String, List<Result>> entry: this.values.entrySet()) {
            valueCopy.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(valueCopy);
    }

    /**
     * Return a ResultMap which contains the merged result of the entries of
     * this ResultMap and the one passed as an argument.
     * In either side {@code isClean}, returns the other side
     * (though identity should not be relied upon).
     * @param right The ResultMap to be merged with this ResultMap.
     * @return A ResultMap containing the merged entries of this and right.
     * @throws {@link NullPointerException} if right is null.
     */
    @EnsuresNonNull({"#1"})
    ResultMap plus(ResultMap right) {
        if (right == null) throw new NullPointerException("Cannot accept null");
        if (right.isClean()) return this;
        if (this.isClean()) return right;
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
    public boolean equals(@Nullable Object other) {
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
