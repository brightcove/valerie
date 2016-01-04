package val

import spock.lang.Specification

class CheckTest extends Specification {

    def 'closures are coerced'() {
        given:
        def expected = ResultMap.from(['coerce': [new Result('worked', 'worked')]])
        Check testCheck = { value -> expected }

        expect:
        testCheck('anything') == expected
        testCheck instanceof Check
    }

    def 'and performs short-circuiting logical and'() {
        given:
        Set wasCalled = new HashSet<>()
        Check check1 = { value -> wasCalled.add('check1'); ResultMap.from(result1) }
        Check check2 = { value -> wasCalled.add('check2'); ResultMap.from(result2) }
        Check andCheck = check1.and(check2)

        when:
        def result = andCheck('input')
        def expected = ResultMap.from(expectedResult)

        then:
        result == expected
        wasCalled == expectedCalled as Set

        where:
        result1                         | result2                         | expectedResult                  | expectedCalled
        [:]                             | [:]                             | [:]                             | ['check1', 'check2']
        [failed: [new Result('f','f')]] | [:]                             | [failed: [new Result('f','f')]] | ['check1']
        [:]                             | [failed: [new Result('f','f')]] | [failed: [new Result('f','f')]] | ['check1', 'check2']
        [failed: [new Result('f','f')]] | [failed: [new Result('f','f')]] | [failed: [new Result('f','f')]] | ['check1']
    }

    def 'or performs short-cirtcuiting logical or'() {
        given:
        Set wasCalled = new HashSet<>()
        Check check1 = { value -> wasCalled.add('check1'); ResultMap.from(result1) }
        Check check2 = { value -> wasCalled.add('check2'); ResultMap.from(result2) }
        Check orCheck = check1.or(check2)

        when:
        def result = orCheck('input')

        then:
        result == ResultMap.from(expectedResult)
        wasCalled == expectedCalled as Set

        where:
        result1                         | result2                           | expectedResult                    | expectedCalled
        [:]                             | [:]                               | [:]                               | ['check1']
        [failed: [new Result('f','f')]] | [:]                               | [:]                               | ['check1', 'check2']
        [:]                             | [failed: [new Result('f','f')]]   | [:]                               | ['check1']
        [failed: [new Result('f','f')]] | [failed: [new Result('f1','f1')]] | [failed: [new Result('f1','f1')]] | ['check1', 'check2']
    }

    def 'add performs composes without short circuiting'() {
        given:
        Set wasCalled = new HashSet<>()
        Check check1 = { value -> wasCalled.add('check1'); ResultMap.from(result1) }
        Check check2 = { value -> wasCalled.add('check2'); ResultMap.from(result2) }
        Check composedCheck = check1 + check2

        when:
        def result = composedCheck('input')

        then:
        result == ResultMap.from(expectedResult)
        wasCalled == ['check1', 'check2'] as Set

        where:
        result1                           | result2                           | expectedResult
        [:]                               | [:]                               | [:]
        [failed: [new Result('f1','f1')]] | [:]                               | [failed: [new Result('f1','f1')]]
        [:]                               | [failed: [new Result('f2','f2')]] | [failed: [new Result('f2','f2')]]
        [failed: [new Result('f1','f1')]] | [failed: [new Result('f2','f2')]] | [failed: [new Result('f2','f2'),
                                                                                          new Result('f1','f1')]]
    }

    def 'composed checks list constructor stores the list as the members'() {
        given:
        def check1 = Mock(Check)
        def check2 = Mock(Check)
        def childAll = new AllCheck([Mock(Check), Mock(Check)])

        expect:
        new AllCheck([check1, check2, childAll]).members == [check1, check2, childAll]
    }

    def 'composed checks two arg constructor merges children of the same type'() {
        given:
        def (check1, check2, check3, check4) = (1..4).collect{Mock(Check)}
        def child1 = new AllCheck(check1, check2)
        def child2 = new AllCheck([check3, check4, check1])

        expect:
        new AllCheck(child1, child2).members == [check1, check2, check3, check4, check1]
        new AllCheck(check1, child1).members == [check1, check1, check2]
        new AllCheck(check1, check2).members == [check1, check2]
    }

    def 'composed checks two arg constructor does not merges children of different types'() {
        given:
        def (check1, check2, check3, check4) = (1..4).collect{Mock(Check)}
        def child1 = new AndCheck(check1, check2)
        def child2 = new OrCheck([check3, check4, check1])

        expect:
        new AllCheck(child1, child2).members == [child1, child2]
    }
}
