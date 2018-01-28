package val

import spock.lang.Specification

class ResultMapNonNullSpecification extends Specification {
    def 'from(map) throws NPE if argument is null'() {
        when:
	ResultMap.from(null)

	then:
	thrown(NullPointerException)
    }

    def 'from(map) throws NPE if any map value is null'() {
    	when:
	ResultMap.from(foo: null)

	then:
	thrown(NullPointerException)
    }

    def 'from(map) throws NPE if any map key is null'() {
    	when:
	ResultMap.from((null): [new Result('foo', 'foo')])

	then:
	thrown(NullPointerException)
    }

    def 'from(map) throws NPE if any list element value is null'() {
    	when:
	ResultMap.from(foo: [null])

	then:
	thrown(NullPointerException)
    }

    def 'from(key, results) throws NPE if either argument is null'() {
    	when:
	ResultMap.from(key, results)

	then:
	thrown(NullPointerException)

	where:
	key	| results
	null	| null
	'foo'	| null
	null	| []
    }

    def 'plus throws NPE if argument is null'() {
    	when:
	ResultMap.from([:]).plus(null)

	then:
	thrown(NullPointerException)
    }
}

class ResultMapImmutabilitySpecification extends Specification {

    def 'modifying the Map constructor argument does not modify the object'() {
    	given:
	Map map = [foo: [new Result('foo', 'foo')]]
	ResultMap resultMap = ResultMap.from(map)

	when:
	map['bar'] = [new Result('bar', 'bar')]

	then:
	resultMap.asMap() == [foo: [new Result('foo', 'foo')]]
    }

    def 'modifying a constructor argument value does not modify the object'() {
    	given:
	List list = [new Result('foo', 'foo')]
	Map map = [foo: list]
	ResultMap resultMap = ResultMap.from(map)

	when:
	list << new Result('bar', 'bar')

	then:
	resultMap.asMap() == [foo: [new Result('foo', 'foo')]]
    }

    def 'attempts to modify asMap throw UnsupportedOperationException'() {
    	given:
	ResultMap resultMap = ResultMap.from(foo: [new Result('foo', 'foo')])

	when:
	resultMap.asMap()['bar'] = [new Result('bar', 'bar')]

	then:
	thrown(UnsupportedOperationException)
    }

    def 'attempts to modify asMap value throw UnsupportedOperationException'() {
    	given:
	ResultMap resultMap = ResultMap.from(foo: [new Result('foo', 'foo')])

	when:
	resultMap.asMap()['foo'] << new Result('bar', 'bar')

	then:
	thrown(UnsupportedOperationException)
    }
}

class ResultMapReusedObjectsSpecification extends Specification {

    def 'passed/empty ResultMaps will reference the same instance'() {
        expect:
        ResultMap.from([:]).is(ResultMap.from([:]))
        ResultMap.from([:]).is(ResultMap.CLEAN)
    }

    def 'plus does some short cirtcuiting to avoid instantiation'() {
        given:
        ResultMap rm1 = ResultMap.from(a: [new Result('a','a')])

        expect:
        (ResultMap.CLEAN + ResultMap.CLEAN).is(ResultMap.CLEAN)
        (rm1 + ResultMap.CLEAN).is(rm1)
        (ResultMap.CLEAN + rm1).is(rm1)
    }

}

class ResultMapOperationSpecification extends Specification {

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
}

class ResultMapValueObjectSpecification extends Specification {

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
        Map test = [a: [new Result('a','a')]]

        expect:
        ResultMap.from(test).hashCode() == test.hashCode()
        ResultMap.from(test).hashCode() != ResultMap.CLEAN.hashCode()
    }
}
