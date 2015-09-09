package val

import spock.lang.Specification

class DefinitionCheckTest extends Specification {
    def checkers = new val.Checkers()

    def 'construction results in a check created from the provided definition'() {
        expect:
        new DefinitionCheck(checkers, [:], 'key',{ isNotNull() })(null) ==
                val.ResultMap.from([key: [new val.Result('required field cannot be null',
                        val.Result.CODE_REQUIRED_FIELD)]])
    }

    def 'define allows multiple checks to be `all`ed together using provided key by default'() {
        expect:
        new DefinitionCheck(checkers, [:], 'key', {
            define isInstanceOf(Collection)
            define isInstanceOf(String)
        })(1) == val.ResultMap.from([ key: [new val.Result('is not of type Collection', val.Result.CODE_ILLEGAL_VALUE),
                                            new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)] ])
    }

    def 'the last check (the return value) of the definition is defined implicitly'() {
        expect:
        new DefinitionCheck(checkers, [:], 'key', {
            define isInstanceOf(Map)
            isInstanceOf(Collection)  //This one will be ignored
            isInstanceOf(String)
        })(1) == val.ResultMap.from([ key: [new val.Result('is not of type Map', val.Result.CODE_ILLEGAL_VALUE),
                                            new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)] ])
    }

    def 'the default key for the definition can be assigned within the definition'() {
        expect:
        new DefinitionCheck(checkers, [:], 'key', {
            resultKey = 'updated'
            define isInstanceOf(Collection)
            define isInstanceOf(String)})(1) == val.ResultMap.from(
                [ updated: [new val.Result('is not of type Collection', val.Result.CODE_ILLEGAL_VALUE),
                            new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)] ])
    }

    def 'define accepts a map which can be used to define children'() {
        expect:
        new DefinitionCheck(checkers, [:], 'key', {
            define a: { isNotNull() }
            define b: {
                define isInstanceOf(Map)
                define b1: { isNotNull() },
                        b2: { isNotNull() }
            }
        })([a:null, b:[b1:null,b2:'a']]) == val.ResultMap.from(
                [ a: [new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)],
                  b1: [new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)] ])
    }

    def 'subDefine is a sugared version of define which aggregates the path as it goes'() {
        expect:
        new DefinitionCheck(checkers, [:], 'root', {
            subDefine a: { isNotNull() }
            define b: {
                define isInstanceOf(Map)
                subDefine b1: { isNotNull() },
                        b2: { define: isNotNull()
                            subDefine b2A: { isNotNull() }
                        }
            }
        })([a:null, b:[b1:null,b2:[b2A:null]]]) == val.ResultMap.from(
                [ 'root.a': [new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)],
                  'b.b1': [new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)],
                  'b.b2.b2A': [new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)]])
    }

    def 'require establishes preconditions for any defined checks'() {
        expect:
        new DefinitionCheck(checkers, [:], 'root', {
            require isNotNull()
            define isInstanceOf(String)
        })(input) == ResultMap.from(results)

        where:
        input       | results
        null        | [root: [new val.Result('required field cannot be null', val.Result.CODE_REQUIRED_FIELD)]]
        1           | [root: [new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)]]
        'valid'     | [:]
    }

    def 'stashValueAs saves the active input so that it can be referenced by other Checks as stashed.{}'() {
        given:
        def check = new DefinitionCheck(checkers, [:], 'root', {
            stashValueAs 'top'
            define child2: {
                satisfies({ input ->
                    input == stashed.top.child1
                }, 'wrong', 'do not match', 'MISMATCH')
            }
        })

        expect:
        check([child1: 'a', child2: 'a']) == ResultMap.passed()
        check([child1: 'a', child2: 1]) == ResultMap.from(['wrong': [new val.Result('do not match', 'MISMATCH')]])
    }

    def 'withValue(definition) creates nested definition for organization'() {
        given:
        def check = new DefinitionCheck(checkers, [:], 'root', {
            isNotNull() & withValue{
                define isInstanceOf(String)
                define isInstanceOf(Integer)
            }
        })

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
        def check = new DefinitionCheck(checkers, [:], 'root', {
            withValue('a') { isInstanceOf(String) }
        })

        expect:
        check(input) == ResultMap.from(results)

        where:
        input   | results
        [a:'a'] | [:]
        [a:1]   | [a:[new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'withValue(child,resultKey, definition) creates definition for the specified child & specified resultKey'() {
        given:
        def check = new DefinitionCheck(checkers, [:], 'root', {
            withValue('a', 'aKey') { isInstanceOf(String) }
        })

        expect:
        check(input) == ResultMap.from(results)

        where:
        input   | results
        [a:'a'] | [:]
        [a:1]   | [aKey:[new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'withSubValue creates definition for the specified child & builds the resultKey using the present resultKey'() {
        given:
        def check = new DefinitionCheck(checkers, [:], 'root', {
            withSubValue('a') { isInstanceOf(String) }
        })

        expect:
        check(input) == ResultMap.from(results)

        where:
        input   | results
        [a:'a'] | [:]
        [a:1]   | ['root.a':[new val.Result('is not of type String', val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'withEachValue will perform validation on each member in a collection'() {
        given:
        def check = new DefinitionCheck(checkers, [:], 'root', {
            withEachValue{isNotNull()}
        })

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
        def check = new DefinitionCheck(checkers, [:], 'root', {
            withEachValue{ define value: {isNotNull()}}
        })

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
        def check = new DefinitionCheck(checkers, [:], 'root', {
            cond([
                    (isInstanceOf(String))     : { hasSizeLte(5) },
                    (isInstanceOf(Collection)) : { isInstanceOf(Set) }
            ])
        })

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

}
