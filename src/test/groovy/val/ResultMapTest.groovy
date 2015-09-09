package val

import spock.lang.Specification

class ResultMapTest extends Specification {

    def 'references to passed/empty ResultMaps will share the same object in memory'() {
        expect:
        ResultMap.from([:]).is(ResultMap.from([:]))
        ResultMap.from([:]).is(ResultMap.passed())
    }

    def 'asMap returns values and object remains immutable'() {
        given: 'a held map reference'
        Map map = [:]

        when: 'a the reference is modified after construction'
        ResultMap resultMap = ResultMap.from(map)
        map['test'] = [new Result('a', 'a')]

        then: 'the entries within the object are not modified'
        resultMap.asMap() == [:]

        when: 'asMap is called'
        resultMap = ResultMap.from([test: [new Result('a','a')]])
        map = resultMap.asMap()

        then: 'the value is equal to the entries in ResultMap'
        map == [test: [new Result('a','a')]]

        when: 'the reference from asMap is modified'
        map.a = [new Result('a','a')]

        then: 'the entries within the object are not modified'
        resultMap.asMap() == [test: [new Result('a', 'a')]]
    }

    def 'plus does some short cirtcuiting to avoid instantiation'() {
        given:
        ResultMap rm1 = ResultMap.from(a: [new Result('a','a')])

        expect:
        (ResultMap.passed() + ResultMap.passed()).is(ResultMap.passed())
        (rm1 + ResultMap.passed()).is(rm1)
        (ResultMap.passed() + rm1).is(rm1)
    }

    def 'plus merges the entries in the two involved ResultMaps'() {
        given:
        ResultMap rm1 = ResultMap.from([a: [new Result('a1','a1')],
                                        b: [new Result('b','b')],
                                        c: [new Result('c', 'c')]])
        ResultMap rm2 = ResultMap.from([a: [new Result('a2','a2')],
                                        b: [new Result('b','b')],
                                        d: [new Result('d', 'd')]])

        when:
        def sum = rm1 + rm2

        then:
        rm1.asMap() == old(rm1.asMap())
        rm2.asMap() == old(rm2.asMap())
        sum.asMap() == [a: [new Result('a1','a1'), new Result('a2','a2')],
                        b: [new Result('b','b'), new Result('b','b')],
                        c: [new Result('c','c')],
                        d: [new Result('d','d')]]
    }

    def 'equals compares the values'() {
        expect:
        (ResultMap.from(left) == ResultMap.from(right)) == expected

        where:
        left                      | right                     | expected
        [:]                       | [:]                       | true
        [:]                       | [a:[new Result('a','a')]] | false
        [a:[new Result('a','a')]] | [a:[new Result('a','a')]] | true
    }

    def 'equals checks type'() {
        expect:
        ResultMap.from(input) != input

        where:
        input << [ [:],
                   [a:[new Result('a','a')]] ]
    }

    def 'hashCode delegates to values'() {
        given:
        Map test = [a: new Result('a','a')]

        expect:
        ResultMap.from(test).hashCode() == test.hashCode()
        ResultMap.from(test).hashCode() != ResultMap.passed()

    }

}
