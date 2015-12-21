package val

import spock.lang.Specification

class IdatorTest extends Specification {
    def checkers = new val.Checkers()

    def 'construction results in a check created from the provided definition'() {
        expect:
        new Idator(checkers, [:], 'key').using{ isNotNull() }(null) ==
                val.ResultMap.from([key: [new val.Result('required field cannot be null',
                        val.Result.CODE_REQUIRED_FIELD)]])
    }

    def 'define allows multiple checks to be `all`ed together using provided key by default'() {
        expect:
        new Idator(checkers, [:], 'key').using{
            define isInstanceOf(Collection)
            define isInstanceOf(String)
        }(1) == val.ResultMap.from([ key: [new val.Result('is not of type Collection', val.Result.CODE_ILLEGAL_VALUE),
                                           new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)] ])
    }

    def 'the last check (the return value) of the definition is defined implicitly'() {
        expect:
        new Idator(checkers, [:], 'key').using{
            define isInstanceOf(Map)
            isInstanceOf(Collection)  //This one will be ignored
            isInstanceOf(String)
        }(1) == val.ResultMap.from([ key: [new val.Result('is not of type Map', val.Result.CODE_ILLEGAL_VALUE),
                                           new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)] ])
    }

    def 'the default key for the definition can be assigned within the definition'() {
        expect:
        new Idator(checkers, [:], 'key').using{
            resultKey = 'updated'
            define isInstanceOf(Collection)
            define isInstanceOf(String)}(1) == val.ResultMap.from(
                [ updated: [new val.Result('is not of type Collection', val.Result.CODE_ILLEGAL_VALUE),
                            new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)] ])
    }

    def 'define accepts a map which can be used to define children'() {
        expect:
        new Idator(checkers, [:], 'key').using{
            define a: { isNotNull() }
            define b: {
                define isInstanceOf(Map)
                define b1: { isNotNull() },
                        b2: { isNotNull() }
            }
        }([a:null, b:[b1:null,b2:'a']]) == val.ResultMap.from(
                [ a: [new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)],
                  b1: [new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)] ])
    }

    def 'subDefine is a sugared version of define which aggregates the path as it goes'() {
        expect:
        new Idator(checkers, [:], 'root').using{
            subDefine a: { isNotNull() }
            define b: {
                define isInstanceOf(Map)
                subDefine b1: { isNotNull() },
                        b2: { define: isNotNull()
                            subDefine b2A: { isNotNull() }
                        }
            }
        }([a:null, b:[b1:null,b2:[b2A:null]]]) == val.ResultMap.from(
                [ 'root.a': [new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)],
                  'b.b1': [new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)],
                  'b.b2.b2A': [new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)]])
    }

    def 'require establishes preconditions for any defined checks'() {
        expect:
        new Idator(checkers, [:], 'root').using{
            require isNotNull()
            define isInstanceOf(String)
        }(input) == ResultMap.from(results)

        where:
        input       | results
        null        | [root: [new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)]]
        1           | [root: [new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)]]
        'valid'     | [:]
    }

    def 'stashValueAs saves the active input so that it can be referenced by other Checks as stashed.{}'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            stashValueAs 'top'
            define child2: {
                satisfies({ input ->
                    input == stashed.top.child1
                }, 'wrong', 'do not match', 'MISMATCH')
            }
        }

        expect:
        check([child1: 'a', child2: 'a']) == ResultMap.passed()
        check([child1: 'a', child2: 1]) == ResultMap.from(['wrong': [new val.Result('do not match', 'MISMATCH')]])
    }

    def 'withValue(definition) creates nested definition for organization'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            isNotNull() & withValue{
                define isInstanceOf(String)
                define isInstanceOf(Integer)
            }
        }

        expect:
        check(input) == ResultMap.from(results)

        where:
        input  | results
        null   | [root:[new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)]]
        [:]    | [root:[new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE),
                           new val.Result('is not of type Integer', val.Result.CODE_ILLEGAL_VALUE)] ]
        1      | [root:[new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)]]

    }

    def 'withValue(child, definition) creates definition for the specified child'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            withValue('a') { isInstanceOf(String) }
        }

        expect:
        check(input) == ResultMap.from(results)

        where:
        input   | results
        [a:'a'] | [:]
        [a:1]   | [a:[new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'withValue(child,resultKey, definition) creates definition for the specified child & specified resultKey'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            withValue('a', 'aKey') { isInstanceOf(String) }
        }

        expect:
        check(input) == ResultMap.from(results)

        where:
        input   | results
        [a:'a'] | [:]
        [a:1]   | [aKey:[new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'withSubValue creates definition for the specified child & builds the resultKey using the present resultKey'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            withSubValue('a') { isInstanceOf(String) }
        }

        expect:
        check(input) == ResultMap.from(results)

        where:
        input   | results
        [a:'a'] | [:]
        [a:1]   | ['root.a':[new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'withEachValue will perform validation on each member in a collection'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            withEachValue{isNotNull()}
        }

        expect:
        check(input) == ResultMap.from(results)

        where:
        input            | results
        [1,2,3]          | [:]
        [1,null,3]       | ['root':[new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)]]
        [null,null,null] | ['root':[new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD),
                                    new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD),
                                    new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)]]
    }

    def 'withEachValue will iterate over map entries'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            withEachValue{ define value: {isNotNull()}}
        }

        expect:
        check(input) == ResultMap.from(results)

        where:
        input           | results
        [a:1,b:2]       | [:]
        [a:null,b:2]    | ['value':[new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)]]
        [a:null,b:null] | ['value':[new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD),
                                    new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)]]
    }

    def 'cond will evaluate first definition where check matches'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            cond([
                    (isInstanceOf(String))     : { hasSizeLte(5) },
                    (isInstanceOf(Collection)) : { isInstanceOf(Set) }
            ])
        }

        expect:
        check(input) == ResultMap.from(results)

        where:
        input     | results
        1         | [:]
        'a'       | [:]
        '123456'  | [root:[new val.Result('should be no longer than 5', val.Result.CODE_TOO_LONG)]]
        [] as Set | [:]
        []        | [root:[new val.Result('is not of type Set', val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'cond takes an optional closure to define how the tested input will be accessed'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            cond({it?.child}, [
                    (isInstanceOf(String))     : { hasSizeLte(5) },
                    (isInstanceOf(Collection)) : { isInstanceOf(Set) }
            ])
        }

        expect:
        check(input) == ResultMap.from(results)

        where:
        input                                       | results
        [child: 1]                                  | [:]
        [child: 'a' ]                               | [:]
        [child: '123456', a:1, b:2, c:3, d:4, e:5]  | [root:[new val.Result('should be no longer than 5', val.Result.CODE_TOO_LONG)]]
        [child: [] as Set]                          | [root:[new val.Result('is not of type Set', val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'define loads an initial validation scope'() {
        given:
        def definedCheck = new val.Idator().using{isNotNull('test')}

        expect:
        definedCheck(null).asMap().containsKey('test')
        !(definedCheck('a').asMap().containsKey('test'))
    }

    def 'after a checker is registered it can be used in definition closures'() {
        given:
        def testResult = val.ResultMap.from(['test':new val.Result('successful','WOOHOO')])

        when:
        def validator = new val.Idator()
        validator.registerChecker('myTestChecker', { String a ->
            {input -> testResult }
        })

        then:
        validator.using{define myTestChecker('a')}('input') == testResult
    }

    def 'custom registered checkers behave consistently with standard checkers'() {
        given:
        def validator = new val.Idator()
        validator.registerChecker('isRequiredString', { isNotNull() & isInstanceOf(String) & hasSizeGte(1) })

        expect:
        validator.using{
            subDefine child: { isRequiredString() }
            define name: { isRequiredString() }
        }(input) == ResultMap.from(expected)

        where:
        input              | expected
        [child: 'valid']   | [name: [new val.Result('required field cannot be null', 'REQUIRED_FIELD')]]
        [:]                | [name: [new val.Result('required field cannot be null', 'REQUIRED_FIELD')],
                              'root.child': [new val.Result('required field cannot be null', 'REQUIRED_FIELD')]]

    }

}