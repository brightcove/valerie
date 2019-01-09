package val

import spock.lang.Specification

class CheckTest extends Specification {
    EvalContext ctx = new EvalContext()

    def asResultMap(Map input) {
        ResultMap.from(input.collectEntries{k,v ->
            [ (k): v.collect{it as Result} ]
        });
    }

    def 'closures are coerced'() {
        given:
        def expected = ResultMap.from(
            ['coerce': [new Result('worked', 'worked')]])
        Check testCheck = { value, context -> expected }

        expect:
        testCheck('anything', ctx) == expected
        testCheck instanceof Check
    }

    def 'and performs short-circuiting logical and'() {
        given:
        Set wasCalled = new HashSet<>()
        Check check1 = { value, context ->
            wasCalled << 1
            asResultMap(result1)
        }
        Check check2 = { value, context ->
            wasCalled << 2
            asResultMap(result2)
        }
        Check andCheck = check1.and(check2)

        when:
        def result = andCheck('input', ctx)
        def expected = asResultMap(expectedResult)

        then:
        result == expected
        wasCalled == calls as Set

        where:
        result1          | result2          | expectedResult   | calls
        [:]              | [:]              | [:]              | [1, 2]
        [f: [['f','f']]] | [:]              | [f: [['f','f']]] | [1]
        [:]              | [f: [['f','f']]] | [f: [['f','f']]] | [1, 2]
        [f: [['f','f']]] | [f: [['f','f']]] | [f: [['f','f']]] | [1]
    }

    def 'or performs short-cirtcuiting logical or'() {
        given:
        Set wasCalled = new HashSet<>()
        Check check1 = { value, context ->
            wasCalled << 1
            asResultMap(result1)
        }
        Check check2 = { value, context ->
            wasCalled << 2
            asResultMap(result2)
        }
        Check orCheck = check1.or(check2)

        when:
        def result = orCheck('input', ctx)

        then:
        result == asResultMap(expectedResult)
        wasCalled == calls as Set

        where:
        result1          | result2            | expectedResult     | calls
        [:]              | [:]                | [:]                | [1]
        [f: [['f','f']]] | [:]                | [:]                | [1,2]
        [:]              | [f: [['f','f']]]   | [:]                | [1]
        [f: [['f','f']]] | [f: [['f1','f1']]] | [f: [['f1','f1']]] | [1, 2]
    }

    def 'add performs composes without short circuiting'() {
        given:
        Set wasCalled = new HashSet<>()
        Check check1 = { value, context ->
            wasCalled << 1
            asResultMap(result1)
        }
        Check check2 = { value, context ->
            wasCalled << 2
            asResultMap(result2)
        }
        Check composedCheck = check1 + check2

        when:
        def result = composedCheck('input', ctx)

        then:
        result == asResultMap(expectedResult)
        wasCalled == [1, 2] as Set

        where:
        result1            | result2            | expectedResult
        [:]                | [:]                | [:]
        [f: [['f1','f1']]] | [:]                | [f: [['f1','f1']]]
        [:]                | [f: [['f2','f2']]] | [f: [['f2','f2']]]
        [f: [['f1','f1']]] | [f: [['f2','f2']]] | [f: [['f1','f1'],
                                                       ['f2','f2']]]
    }
}
