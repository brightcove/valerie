package val

import spock.lang.Specification

class IdatorTest extends Specification {
    def checkers = new val.Checkers()

    def 'using creates Check out of definition'() {
        given:
        Check check = new Idator(checkers, [:], 'key').using{ isNotNull() }

        expect:
        check(null) == val.ResultMap.from(
            [key: [new val.Result('required field cannot be null',
                                  val.Result.CODE_REQUIRED_FIELD)]])
    }

    def 'define allows checks to be `all`ed using provided key by default'() {
        given:
        Check check = new Idator(checkers, [:], 'key').using{
            define isInstanceOf(Collection)
            define isInstanceOf(String)
        }

        expect:
        check(1) == val.ResultMap.from(
            [ key: [new val.Result('is not of type Collection',
                                   val.Result.CODE_ILLEGAL_VALUE),
                    new val.Result('is not of type String',
                                   val.Result.CODE_ILLEGAL_VALUE)] ])
    }

    def 'a return value Check of a definition is defined implicitly'() {
        given:
        Check check = new Idator(checkers, [:], 'key').using{
            define isInstanceOf(Map)
            isInstanceOf(Collection) //This one will be ignored
            isInstanceOf(String)
        }

        expect:
        check(1) == val.ResultMap.from(
            [ key: [new val.Result('is not of type Map',
                                   val.Result.CODE_ILLEGAL_VALUE),
                    new val.Result('is not of type String',
                                   val.Result.CODE_ILLEGAL_VALUE)] ])
    }

    def 'the default key for the current definition is set with resultKey'() {
        given:
        Check check = new Idator(checkers, [:], 'key').using {
            resultKey = 'updated'
            define isInstanceOf(Collection)
            define isInstanceOf(String)
        }

        expect:
        check(1) == val.ResultMap.from(
                [ updated: [new val.Result('is not of type Collection',
                                           val.Result.CODE_ILLEGAL_VALUE),
                            new val.Result('is not of type String',
                                           val.Result.CODE_ILLEGAL_VALUE)] ])
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
                [ a: [new val.Result('required field cannot be null',
                                     val.Result.CODE_REQUIRED_FIELD)],
                 b1: [new val.Result('required field cannot be null',
                                    val.Result.CODE_REQUIRED_FIELD)] ])
    }

    def 'subDefine is define which constructs the path as it goes'() {
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
                [ 'root.a': [new val.Result('required field cannot be null',
                                            val.Result.CODE_REQUIRED_FIELD)],
                  'b.b1': [new val.Result('required field cannot be null',
                                          val.Result.CODE_REQUIRED_FIELD)],
                  'b.b2.b2A': [new val.Result('required field cannot be null',
                                              val.Result.CODE_REQUIRED_FIELD)]])
    }

    def 'require establishes preconditions for any defined checks'() {
        expect:
        new Idator(checkers, [:], 'root').using{
            require isNotNull()
            define isInstanceOf(String)
        }(input) == ResultMap.from(results)

        where:
        input       | results
        null        | [root: [new val.Result('required field cannot be null',
                                             val.Result.CODE_REQUIRED_FIELD)]]
        1           | [root: [new val.Result('is not of type String',
                                             val.Result.CODE_ILLEGAL_VALUE)]]
        'valid'     | [:]
    }

    def 'stashValueAs saves the active input for later stashed.{} reference'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            stashValueAs 'top'
            define child2: {
                satisfies(key:'wrong',
                    msg:'do not match',code:'MISMATCH'){ input ->
                        input == stashed.top.child1
                }
            }
        }

        expect:
        check([child1: 'a', child2: 'a']) == ResultMap.passed()
        check([child1: 'a', child2: 1]) == ResultMap.from(
            ['wrong': [new val.Result('do not match', 'MISMATCH')]])
    }

    def 'withValue(def) creates nested definition for organization'() {
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
        null   | [root:[new val.Result('required field cannot be null',
                                       val.Result.CODE_REQUIRED_FIELD)]]
        [:]    | [root:[new val.Result('is not of type String',
                                       val.Result.CODE_ILLEGAL_VALUE),
                           new val.Result('is not of type Integer',
                                          val.Result.CODE_ILLEGAL_VALUE)] ]
        1      | [root:[new val.Result('is not of type String',
                                       val.Result.CODE_ILLEGAL_VALUE)]]

    }

    def 'withValue(child,def) defines for the specified child'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            withValue('a') { isInstanceOf(String) }
        }

        expect:
        check(input) == ResultMap.from(results)

        where:
        input   | results
        [a:'a'] | [:]
        [a:1]   | [a:[new val.Result('is not of type String',
                                     val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'withValue(child,rKey,def) defines child & resultKey'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            withValue('a', 'aKey') { isInstanceOf(String) }
        }

        expect:
        check(input) == ResultMap.from(results)

        where:
        input   | results
        [a:'a'] | [:]
        [a:1]   | [aKey:[new val.Result('is not of type String',
                                        val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'withSubValue defines child & builds resultKey using present key'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            withSubValue('a') { isInstanceOf(String) }
        }

        expect:
        check(input) == ResultMap.from(results)

        where:
        input   | results
        [a:'a'] | [:]
        [a:1]   | ['root.a':[new val.Result('is not of type String',
                                            val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'withEachValue will validate on each member in a collection'() {
        given:
        def check = new Idator(checkers, [:], 'root').using{
            withEachValue{isNotNull()}
        }

        expect:
        check(input) == ResultMap.from(results)

        where:
        input            | results
        [1,2,3]          | [:]
        [1,null,3]       | ['root':[
                new val.Result('required field cannot be null',
                               val.Result.CODE_REQUIRED_FIELD)]]
        [null,null,null] | ['root':[
                new val.Result('required field cannot be null',
                               val.Result.CODE_REQUIRED_FIELD),
                new val.Result('required field cannot be null',
                               val.Result.CODE_REQUIRED_FIELD),
                new val.Result('required field cannot be null',
                               val.Result.CODE_REQUIRED_FIELD)]]
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
        [a:null,b:2]    | ['value':[
                new val.Result('required field cannot be null',
                               val.Result.CODE_REQUIRED_FIELD)]]
        [a:null,b:null] | ['value':[
                new val.Result('required field cannot be null',
                               val.Result.CODE_REQUIRED_FIELD),
                new val.Result('required field cannot be null',
                               val.Result.CODE_REQUIRED_FIELD)]]
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
        '123456'  | [root:[new val.Result('should be no longer than 5',
                                          val.Result.CODE_TOO_LONG)]]
        [] as Set | [:]
        []        | [root:[new val.Result('is not of type Set',
                                          val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'cond takes an optional closure returning the test input'() {
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
        [child: '123456', a:1, b:2, c:3, d:4, e:5]  | [root:[
                new val.Result('should be no longer than 5',
                               val.Result.CODE_TOO_LONG)]]
        [child: [] as Set]                          | [root:[
                new val.Result('is not of type Set',
                               val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'define loads an initial validation scope'() {
        given:
        def definedCheck = new val.Idator().using{isNotNull(key:'test')}

        expect:
        definedCheck(null).asMap().containsKey('test')
        !(definedCheck('a').asMap().containsKey('test'))
    }

    def 'a registered checker can be used in definition closures'() {
        given:
        def testResult = val.ResultMap.from(
            ['test':new val.Result('successful','WOOHOO')])

        when:
        def validator = new val.Idator()
        validator.registerChecker('myTestChecker', { String a ->
            {input -> testResult }
        })

        then:
        validator.using{define myTestChecker('a')}('input') == testResult
    }

    def 'custom registered checkers behave like standard checkers'() {
        given:
        def validator = new val.Idator()
        validator.registerChecker('isRequiredString',
                                  { isNotNull() & isInstanceOf(String) &
                                      hasSizeGte(1) })

        expect:
        validator.using{
            subDefine child: { isRequiredString() }
            define name: { isRequiredString() }
        }(input) == ResultMap.from(expected)

        where:
        input              | expected
        [child: 'valid']   | [name: [
                new val.Result('required field cannot be null',
                               'REQUIRED_FIELD')]]
        [:]                | [name: [
                new val.Result('required field cannot be null',
                               'REQUIRED_FIELD')
            ], 'root.child': [new val.Result('required field cannot be null',
                                             'REQUIRED_FIELD')]]
    }
}
