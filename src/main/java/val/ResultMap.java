package val;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An immutable value object which conveys the result of an evaluation
 * of a {@link Check} against a provided input.
 * This is represented as a Map where the `key` defines the context
 * for specific feedback and the value is a {@link Result} object
 * providing that feedback. In conventional use the key will be the
 * field or complete path to a field for which the {@link Result}
 * applies.
 *
 * If the map is empty then the checks have nothing to say (everything passed)
 * and the ResultMap is considered `clean`.
 **/
public abstract class ResultMap {

    /**
     * Whether the evaluation which created this ResultMap warranted no feedback.
     *
     * @return true if the ResultMap contains no feedback, false otherwise.
     **/
    public abstract boolean isClean();

    // isClean is called heavily during composition of checks and is expected
    // to be called most often against the CLEAN flyweight.
    // This sealed class approach optimizes out needless comparisons for that path.
    private static final class CleanResultMap extends ResultMap {
	private CleanResultMap() {
	    super(new LinkedHashMap<>(0));
	}
	public boolean isClean() { return true; }
    }

    private static final class StandardResultMap extends ResultMap {
	private StandardResultMap(final Map<String, List<Result>> values) {
	    super(values);
	}
	public boolean isClean() { return this.values.isEmpty(); }
    }

    /**
     * The results this ResultMap represents.
     *
     * The keys identify the path or similar context
     * and the values are the list of {@link Result}s for that context.
     **/
    final Map<String, List<Result>> values;

    /**
     * An empty ResultMap to be used when there is no feedback to provide.
     *
     * This instance is used as a flyweight as it is expected to be the
     * result for the majority of evaluations (but the flyweight identity
     * should not be relied upon in any client code).
     **/
    final static ResultMap CLEAN = new CleanResultMap();

    /**
     * Return a ResultMap containing the entries in the provided Map.
     *
     * This is the standard way of creating a ResultMap.
     * 
     * @param map A Map with entries where the keys are context
     *            and the values are the list of Results.
     * @return A ResultMap with the entries provided.
     * @throws NullPointerException if map is null.
     **/
    @EnsuresNonNull({"#1"})
    public static ResultMap from(final Map<String, List<Result>> map) {
	if (map == null) throw new NullPointerException("map must not be null");
	if (map.isEmpty()) return CLEAN;
	return new StandardResultMap(map);
    }

    /**
     * Return a ResultMap containing one entry with the provided key and results.
     *
     * @param key The String key for the entry in the ResultMap.
     * @param results The list of Results to associate with the provided key.
     * @return A single key ResultMap containing the provided entry.
     * @throws NullPointerException if either argument is null.
     **/
    @EnsuresNonNull({"#1", "#2"})
    public static ResultMap from(final String key, final List<Result> results) {
	if (key == null) throw new NullPointerException("key must not be null");
	if (results == null)
	    throw new NullPointerException("results must not be null");
	Map<String, List<Result>> map = new LinkedHashMap<>();
	map.put(key, results);
	return ResultMap.from(map);
    }

    /*
     * Internal constructor which ensures immutability of contained content.
     */
    private ResultMap(final Map<String, List<Result>> arg) {
	// Validate that provided map does not contain any null pointers,
	// while copying to a structure that can be typed as such.
	Map<String, List<Result>> values =
	    new LinkedHashMap<String, List<Result>>(arg.size());
	for (Map.Entry<String, List<Result>> entry: arg.entrySet()) {
	    if (entry.getKey() == null)
		throw new NullPointerException("Null keys are prohibited.");
	    List<Result> from = entry.getValue();
	    if (from == null)
		throw new NullPointerException("Null values are prohibited.");
	    List<Result> results = new ArrayList<Result>(from.size());
	    for (Result result: from) {
		if (result == null)
		    throw new NullPointerException("Null values are prohibited");
		results.add(result);
	    }
	    values.put(entry.getKey(), Collections.unmodifiableList(results));
	}
	this.values = Collections.unmodifiableMap(values);
    }

    /**
     * Return a read-only representation of this ResultMap as a Map.
     *
     * Note <em>read-only</em> above. Modifying the data within the
     * returned map is not supported (and will result in an exception).
     * A (deep) copy should be made if any modifications are desired.
     *
     * @return A Map containing the entries within this ResultMap.
     **/
    public Map<String, List<Result>> asMap() {
	return values;
    }

    /**
     * Get the combined sum of the current ResultMap with the provided argument.
     *
     * Return a ResultMap which contains the merged result of the entires of
     * this ResultMap and the one passed as an argument.
     *
     * @param other The ResultMap to merge with this ResultMap
     * @return A ResultMap containing the combined results of this and other.
     * @throws NullPointerException if right is null.
     **/
    @EnsuresNonNull({"#1"})
    ResultMap plus(final ResultMap other) {
	if (other == null)
	    throw new NullPointerException("Null argument is prohibited.");
	if (other.isClean()) return this;
	if (this.isClean()) return other;
	Map<String, List<Result>> merged = new LinkedHashMap<>(this.values.size());
	mapCopy(merged, this.values);
	mapCopy(merged, other.values);
	return new StandardResultMap(merged);
    }

    // Facilitate deep copies
    private void mapCopy(final Map<String, List<Result>> dest,
			 final Map<String, List<Result>> src) {
	for (Map.Entry<String, List<Result>> e: src.entrySet()) {
	    String k = e.getKey();
	    List<Result> vals
		= dest.getOrDefault(k, new ArrayList<>(e.getValue().size()));
	    vals.addAll(e.getValue());
	    dest.put(k, vals);
	}
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
