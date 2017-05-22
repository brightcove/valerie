package val;

import java.util.*;
import java.util.regex.*;

import org.checkerframework.checker.nullness.qual.*;

/**
 * Factory class which produces {@link Check}s implementing
 * defined validations and (soon) provides additional context
 * for use in the {@code mold} of that Check.
 */
public class CheckFactory {

    /**
     * Return a passing ResultMap.
     * @return Check that returns ResultMap.passed()
     */
    public Check pass() {
        return Check.from((input, ctx) -> { return ResultMap.CLEAN; });
    }

    /**
     * Return ResultMap constructed from provided arguments
     * @param mold Mold for ResultMap
     * @return Check which will return ResultMap with constructed Result
     */
    public Check fail(Map args) {
        Mold mold = moldify(args)
            .withDefault("key", "fail")
            .withDefault("msg", "failed because you said so")
            .withDefault("code", Result.CODE_ILLEGAL_VALUE);
        return Check.from((input, ctx) -> {
                return ResultMap.from(
                    mold.get("key"),
                    Arrays.asList(new Result(mold.get("msg"),
                                             mold.get("code"))));
            });
    }
    public Check fail() { return fail(new HashMap<String, String>()); }

    /**
     * Validate that the string representation of input matches the pattern
     * specified (implicitly anchored)
     *
     * This will match using implicit stringifying (toString)
     * so things like arrays _could_ be tested (but probably shouldn't)
     * @param pattern String representation of regular expression to match
     * @param mold Mold for ResultMap if input does not match
     */
    @EnsuresNonNull({"#1", "#2"})
    public Check includesPattern(Map args, String pattern) {
        if (args == null) throw new NullPointerException("args must not be null");
        if (pattern == null) throw new NullPointerException("pattern must not be null");
        Pattern regex = Pattern.compile(pattern);
        return satisfies(moldify(args)
                         .withDefault("msg", "does not match required pattern")
                         .withDefault("code", Result.CODE_ILLEGAL_VALUE),
                         (input, ctx) -> {
                             return (input == null) ||
                                 regex.matcher(input.toString()).matches();
                         });
    }

    //
    //Generalized building block type functions
    //
    /**
     * Invokes predicate function test with input and if the test returns false
     * returns a ResultMap containing an entry with a key of key and a Result
     * having message and code
     * @param test Closure which accepts the input and returns a Boolean to
     * determine whether to call onFail
     * @param mold Mold used to create ResultMap when !test
     */
    @EnsuresNonNull({"#1", "#2"})
    public Check satisfies(Map mold,
                           CheckFunk<Boolean> test) {
        if (mold == null) throw new NullPointerException("mold must not be null");
        if (test == null) throw new NullPointerException("test must not be null");
        Map localMold = mold;
        return satisfies(mold, test, (input, ctx) -> {
                if (localMold.get("key") == null)
                    throw new NullPointerException("key must not be null");
                if (localMold.get("msg") == null)
                    throw new NullPointerException("msg must not be null");
                if (localMold.get("code") == null)
                    throw new NullPointerException("code must not be null");
                return ResultMap
                    .from(localMold.get("key").toString(),
                          new ArrayList<>(Arrays
                                          .asList(new Result(localMold.get("msg").toString(),
                                                             localMold.get("code").toString()))));
            });
    }

    /**
     * Invokes predicate function test passing input as an argument and
     * returns onFail result if non-truthy
     * otherwise ResultMap.passed()
     * @param test Closure which returns a Boolean based on the value to
     * determine whether to call onFail
     * @param onFail Closure which takes the input and returns the ResultMap
     * which should indicate test failure
     * @return Check to evaluate input
     */
    @EnsuresNonNull({"#1","#2","#3"})
    public Check satisfies(Map mold,
                           CheckFunk<Boolean> test,
                           CheckFunk<ResultMap> onFail) {
        if (mold == null) throw new NullPointerException("mold must not be null");
        if (test == null) throw new NullPointerException("test must not be null");
        if (onFail == null) throw new NullPointerException("onFail must not be null");
        CheckFunk<Boolean> localTest = test;
        CheckFunk<ResultMap> localOnFail = onFail;
        Check newCheck = new Check() {
                @Override
                public ResultMap call(Object input,
                                      EvalContext ctx) {
                    return localTest.call(input, ctx) ?
                        ResultMap.CLEAN : localOnFail.call(input, ctx);
                }
            };
        newCheck.setMold(mold);
        return newCheck;
    }

    //
    // COMBINATORS
    // Used to return a new check that is a combined version of provided checks
    // All other functions should not expect collections of closures as client
    // code can use the appropriate combinator
    //
    /**
     * Combine sequence of provided checks, execute all of them, and return
     * merged results
     * @param rules Sequence of Checks, all of which wil be executed
     * @return Composed Check to evaluate input and return the merged ResultMap
     */
    @EnsuresNonNull({"#1"})
    public Check all(Check... checks) { return new AllCheck(asNonNullList(checks)); }

    /**
     * Combine sequence of provided checks with a short circuited logical `and`
     * If any check returns a non passing ResultMap, then stop iterating and
     * return that ResultMap
     * @param checks Sequence of Checks to evaluate in order
     * @return Composed Check to evaluate input and return the first
     * non-passing ResultMap else ResultMap.passed()
     */
    public Check and(Check... checks) { return new AndCheck(asNonNullList(checks)); }

    /**
     * Combine sequence of provided checks with a short circuited logical 'or'
     * If any check returns a passing ResultMap, than stop iterating and return
     * that ResultMap
     * If no checks return a passing ResultMap, then return the last ResultMap
     * @param checks Sequence of Checks to evaluate in order
     * @return Composed Check to evaluate input and return the first
     * ResultMap.passed() or last non-passing
     */
    @EnsuresNonNull({"#1"})
    public Check or(Check... checks) { return new OrCheck(asNonNullList(checks)); }

    @EnsuresNonNull({"#1"})
    private List<? extends Check> asNonNullList(Check... checks) {
        if (checks == null) throw new NullPointerException("checks must not be null");
        for (Check check: checks)
            if (check == null) throw new NullPointerException("All checks must not be null");
        return Arrays.asList(checks);
    }

    public Mold moldify(Map<String, String> values) {
        if (values instanceof Mold) return (Mold) values;
        return new Mold(values);
    }

    public class Mold implements Map<String, String> {
        private final Map<String, String> inner;

        private Mold(Map<String, String> input) {
            if (input == null)
                throw new NullPointerException("input is required");
            this.inner = input;
        }

        @EnsuresNonNull({"#1", "#2"})
        public Mold withDefault(String key, String value) {
            if (key == null) throw new NullPointerException("key must not be null");
            if (value == null) throw new NullPointerException("value must not be null");
            if (!inner.containsKey(key)) inner.put(key, value);
            return this;
        }

        @Override
        @SuppressWarnings("keyfor") //Implicitly trust internal Map implementation
        public Set<Map.Entry<@KeyFor("this") String, String>> entrySet() {
            return inner.entrySet();
        }

        @Override
        public Collection<String> values() {
            return inner.values();
        }

        @Override
        @SuppressWarnings("keyfor") //Implicitly trust internal Map implementation
        public Set<@KeyFor("this") String> keySet() {
            return inner.keySet();
        }

        @Override
        public void clear() {
            inner.clear();
        }

        @Override
        @EnsuresNonNull({"#1"})
        public void putAll(Map<? extends String, ? extends String> m) {
            if (m == null) throw new NullPointerException("m must not be null");
            inner.putAll(m);
        }

        @Override
        public @Nullable String remove(Object key) {
            return inner.remove(key);
        }

        @Override
        @EnsuresNonNull({"#1", "#2"})
        public @Nullable String put(String key, String value) {
            if (key == null) throw new NullPointerException("key must not be null");
            if (value == null) throw new NullPointerException("value must not be null");
            return inner.put(key, value);
        }

        @Override
        public String get(Object key) {
            String value = inner.get(key);
            if (value == null) throw new NullPointerException("Requested key has no value");
            return value;
        }

        @Override
        public boolean containsValue(Object value) {
            return inner.containsValue(value);
        }

        @Override
        public boolean containsKey(Object key) {
            return inner.containsKey(key);
        }

        @Override
        public boolean isEmpty() {
            return inner.isEmpty();
        }

        @Override
        public int size() {
            return inner.size();
        }
    }
}
