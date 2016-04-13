package val

import spock.lang.Specification
import spock.lang.Unroll

class CheckersTest extends Specification {

    def v = new Checkers()
    EvalContext ctx = new EvalContext()

    def 'pass returns passed'() {
        expect:
        v.pass()('anything', ctx) == ResultMap.passed()
    }

    def 'fail => entry'() {
        expect:
        v.fail()('anything', ctx) ==
            ResultMap.from('fail': [new Result('failed because you said so',
                                               Result.CODE_ILLEGAL_VALUE)])
        v.fail(key:'foo', msg:'bar', code:'FOO_BAR')('anything', ctx) ==
            ResultMap.from('foo': [new Result('bar', 'FOO_BAR')])
    }

    def 'hasMember => entry if member is not present'() {
        given:
        Check check = v.hasMember(attribute, key:'required')

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input            | attribute  | results
        [a:'test']       | 'a'        | [:]
        [a: null]        | 'a'        | [:]
        [:]              | 'a'        | [required:[
                new val.Result('required field a is not present',
                               val.Result.CODE_REQUIRED_FIELD)]]
    }

    def 'hasOnlyFieldsIn => ILLEGAL_VALUE if not map'() {
        given:
        Check check = v.hasOnlyFieldsIn(['a','b','c'] as Set)

        expect:
        check('invalid', ctx) == ResultMap.from(
                [(null): [new Result('only maps are supported',
                        'ILLEGAL_VALUE')]])
    }

    def 'hasOnlyFieldsIn => entry for each field not in set'() {
        given:
        Check check = v.hasOnlyFieldsIn(['a','b','c'] as Set)

        expect:
        check (input, ctx) == ResultMap.from(results)

        where:
        input                 | results
        [a:1]                 | [:]
        [a:1,b:2,c:3]         | [:]
        [a:1,b:2,c:3,d:4,e:5] | [
        d:[new val.Result('field is unknown',
                          val.Result.CODE_ILLEGAL_FIELD)],
        e:[new val.Result('field is unknown',
                          val.Result.CODE_ILLEGAL_FIELD)]
        ]
    }

    class SampleClass {
        def a, b, c
    }
    def 'hasOnlyFieldsIn => entry for each field not in class'() {
        given:
        Check check = v.hasOnlyFieldsIn(SampleClass)

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input                 | results
        [a:1]                 | [:]
        [a:1,b:2,c:3]         | [:]
        [a:1,b:2,c:3,d:4,e:5] | [
        d:[new val.Result('field is unknown', val.Result.CODE_ILLEGAL_FIELD)],
        e:[new val.Result('field is unknown', val.Result.CODE_ILLEGAL_FIELD)] ]
    }

    def 'hasOnlyFieldsIn builds keys from $key.$field'() {
        given:
        Check check = v.hasOnlyFieldsIn(['a','b',3] as Set, key:'parent')

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input                 | results
        [a:1,b:2,3:3,d:4,e:5] | [
        'parent.d':[
            new val.Result('field is unknown',
                           val.Result.CODE_ILLEGAL_FIELD)],
        'parent.e':[new val.Result('field is unknown',
                                   val.Result.CODE_ILLEGAL_FIELD)]
        ]
    }

    def 'hasSizeGte => entry if size < min'() {
        given:
        Check check = v.hasSizeGte(1, key:'value')

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input        | results
        null         | [:]
        '1'          | [:]
        [1]          | [:]
        [a:1]        | [:]
        ''           | [value:[new val.Result('should be at least 1 long',
                                              val.Result.CODE_TOO_SHORT)]]
        []           | [value:[new val.Result('should be at least 1 long',
                                              val.Result.CODE_TOO_SHORT)]]
        [:]          | [value:[new val.Result('should be at least 1 long',
                                              val.Result.CODE_TOO_SHORT)]]
    }

    @Unroll
    def 'hasSizeLte => entry if size > max'() {
        given:
        Check check = v.hasSizeLte(3, key:'value')

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input              | results
        null               | [:]
        '123'              | [:]
        [1,2,3]            | [:]
        [a:1,b:2,c:3]      | [:]
        '1234'             | [value:[
                new val.Result('should be no longer than 3',
                               val.Result.CODE_TOO_LONG)]]
        [1,2,3,4]          | [value:[
                new val.Result('should be no longer than 3',
                               val.Result.CODE_TOO_LONG)]]
        [a:1,b:2,c:3,d:4]  | [value:[
                new val.Result('should be no longer than 3',
                               val.Result.CODE_TOO_LONG)]]
    }

    def 'hasValueGte => entry if value < min'() {
        given:
        Check check = v.hasValueGte(min,key:'val')

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        min    | input   | results
        5      | 6       | [:]
        5      | 5       | [:]
        5      | 4       | [val:[new val.Result('should not be less than 5',
                                                val.Result.CODE_ILLEGAL_VALUE)]]
        'G'    | 'G'     | [:]
        'G'    | 'g'     | [:]
        'G'    | 'a'     | [:]
        'G'    | 'A'     | [val:[new val.Result('should not be less than G',
                                                val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'hasValueLte => entry if value > max'() {
        given:
        Check check = v.hasValueLte(max,key:'val',msg:'should be less than max')

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        max        | input          | results
        5          | 4              | [:]
        5          | 5              | [:]
        5          | 6              | [val: [
                new val.Result('should be less than max',
                               val.Result.CODE_ILLEGAL_VALUE)]]
        new Date() | new Date() - 1 | [:]
        new Date() | new Date() + 1 | [val: [
                new val.Result('should be less than max',
                               val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'isInstanceOf => entry if not of type'() {
        given:
        Check check = v.isInstanceOf(type, key:'type')

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input       | type         | results
        null        | String       | [:]
        'test'      | String       | [:]
        "${'test'}" | String       | [type:[
                new val.Result('is not of type String', 'ILLEGAL_VALUE')]]
        'test'      | Integer      | [type:[
                new val.Result('is not of type Integer', 'ILLEGAL_VALUE')]]
        []          | Iterable     | [:]
        []          | ArrayList    | [:]
        []          | Map          | [type:[
                new val.Result('is not of type Map', 'ILLEGAL_VALUE')]]
    }

    def 'isOneOf => entry if not in set'() {
        given:
        Check check = v.isOneOf(allowed, key:'value')

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input         | allowed   | results
        'any'         | ['any']   | [:]
        []            | ['any']   | [value:[
                new val.Result('is not one of allowed values: [any]',
                               'ILLEGAL_VALUE')]]
        [:]           | [[:]]     | [:]
        []            | [[]]      | [:]
        []            | [['test']]| [value:[
                new val.Result('is not one of allowed values: [[test]]',
                               'ILLEGAL_VALUE')]]
        'not'         | ['any']   | [value:[
                new val.Result('is not one of allowed values: [any]',
                               'ILLEGAL_VALUE')]]
        1             | [2,4]     | [value:[
                new val.Result('is not one of allowed values: [2, 4]',
                               'ILLEGAL_VALUE')]]
        2             | [2,4]     | [:]
        null          | [null]    | [:]
    }

    ///could use a standard enum but I don't know any off the top of my head
    enum Test {A, B, C}
    def 'isOneOf => entry if not enum value'() {
        given:
        Check check = v.isOneOf(Test, key:'value')

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input        | results
        Test.A       | [:]
        'C'          | [:]
        'D'          | [value:[
                new val.Result('is not one of allowed values: [A, B, C]',
                               'ILLEGAL_VALUE')]]
    }

    def 'isNotNull => entry if null'() {
        given:
        Check check = v.isNotNull(key: 'required')

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input     | results
        'valid'   | [:]
        ''        | [:]
        0         | [:]
        false     | [:]
        null      | ['required':[new val.Result('required field cannot be null',
                                                'REQUIRED_FIELD')]]
    }

    def 'isNull => entry if not null'() {
        given:
        Check check = v.isNull(key:'required')

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input     | results
        null      | [:]
        'bad'     | ['required':[new val.Result('field must be null',
                                                'ILLEGAL_VALUE')]]
        ''        | ['required':[new val.Result('field must be null',
                                                'ILLEGAL_VALUE')]]
        0         | ['required':[new val.Result('field must be null',
                                                'ILLEGAL_VALUE')]]
        [:]       | ['required':[new val.Result('field must be null',
                                                'ILLEGAL_VALUE')]]
    }

    def 'matchesRe checks whether value matches provided regular expression'() {
        given:
        Check check = v.matchesRe(pattern, key:'value')

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input        | pattern   | results
        null         | /\d+/     | [:]
        '21'         | /\d+/     | [:]
        ''           | /\d+/     | [value:[
                new val.Result('does not match required pattern',
                               'ILLEGAL_VALUE')]]
        'a'          | /\d+/     | [value:[
                new val.Result('does not match required pattern',
                               'ILLEGAL_VALUE')]]
        'yes'        | /y\w+/    | [:]
        'other'      | /y\w+/    | [value:[
                new val.Result('does not match required pattern',
                               'ILLEGAL_VALUE')]]
        'oyeah'      | /y\w+/    | [value:[
                new val.Result('does not match required pattern',
                               'ILLEGAL_VALUE')]]
        123          | /\d+/     | [:]
        123          | /y\w+/    | [value:[
                new val.Result('does not match required pattern',
                               'ILLEGAL_VALUE')]]
        ['1']        | /\d+/     | [value:[
                new val.Result('does not match required pattern',
                               'ILLEGAL_VALUE')]]
        ['1']        | /.*\d.*/  | [:]
        [b:'12']     | /.*b.*/   | [:]
        ['b']        | /\d+/     | [value:[
                new val.Result('does not match required pattern',
                               'ILLEGAL_VALUE')]]
        ['yes']      | /y\w+/    | [value:[
                new val.Result('does not match required pattern',
                               'ILLEGAL_VALUE')]]
    }

    def 'satisfies(pattern, test) => test?passed:ResultMap from pattern'() {
        given:
        Check check = v.satisfies({input, context->input}, key:'invalid',
            msg:'uh oh',code:'BAD_VALUE')

        expect:
        check(input, ctx) == ResultMap.from(expected)

        where:
        input     | expected
        true      | [:]
        false     | [invalid: [new val.Result('uh oh', 'BAD_VALUE')]]
    }

    def 'satisfies(test,pattern) expands GStrings late to access input'() {
        given:
        Check check = v.satisfies({input, context -> input}, key:'invalid',
            msg:"$input is not truthy", code:'BAD_VALUE')

        expect:
        check(input, ctx) == ResultMap.from(expected)

        where:
        input     | expected
        true      | [:]
        false     | [invalid: [new val.Result('false is not truthy',
                                              'BAD_VALUE')]]
        []        | [invalid: [new val.Result('[] is not truthy', 'BAD_VALUE')]]
    }



    def 'satisfies(test,onFail) returns onFail() if !test(), else passing'() {
        given:
        Check check = v.satisfies({input, context -> input}, {input, context ->
            ResultMap.from([fail:[new val.Result('fail', 'fail')]])})

        expect:
        check(input, ctx) == ResultMap.from(expected)

        where:
        input     | expected
        true      | [:]
        false     | [fail: [new val.Result('fail', 'fail')]]
        null      | [fail: [new val.Result('fail', 'fail')]]
        0         | [fail: [new val.Result('fail', 'fail')]]
        ''        | [fail: [new val.Result('fail', 'fail')]]
        []        | [fail: [new val.Result('fail', 'fail')]]
    }

    def 'all collects the results of all the provided checks'() {
        given:
        Check check = v.all(
                v.isNotNull(key:'test'),
                v.isInstanceOf(String, key:'test'),
                v.isOneOf(['a','b'], key:'test') )

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input    | results
        null     | [test:[new val.Result('required field cannot be null',
                                         'REQUIRED_FIELD'),
                          new val.Result('is not one of allowed values: [a, b]',
                                         'ILLEGAL_VALUE')]]
        1        | [test:[new val.Result('is not of type String',
                                         'ILLEGAL_VALUE'),
                          new val.Result('is not one of allowed values: [a, b]',
                                         'ILLEGAL_VALUE')]]
        'a'      | [:]
        'c'      | [test:[new val.Result('is not one of allowed values: [a, b]',
                                         'ILLEGAL_VALUE')]]
    }

    def 'and short circuits and returns first non-passing'() {
        given:
        Check check = v.and(
                v.isNotNull(key:'test'),
                v.isInstanceOf(String, key:'test'),
                v.isOneOf(['a','b'], key:'test') )

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input    | results
        null     | [test:[new val.Result('required field cannot be null',
                                         'REQUIRED_FIELD')]]
        1        | [test:[new val.Result('is not of type String',
                                         'ILLEGAL_VALUE')]]
        'a'      | [:]
        'c'      | [test:[new val.Result('is not one of allowed values: [a, b]',
                                         'ILLEGAL_VALUE')]]
    }

    def 'or short circuits or returns last result'() {
        given:
        Check check = v.or(
            v.isInstanceOf(String, key:'test'),
            v.isInstanceOf(Integer, key:'test'))

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input    | results
        null     | [:]
        1        | [:]
        'a'      | [:]
        []       | [test:[new val.Result('is not of type Integer',
                                         'ILLEGAL_VALUE')]]
    }

    //
    // OTHER HIGHER ORDER FUNCTIONS
    //
    def 'when evaluates bodyCheck if testCheck passes'() {
        given:
        Check check = v.when(v.isInstanceOf(Collection, key:'test'),
                           v.hasSizeLte(5, key:'test'))

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input          | results
        null           | [:]
        '1234567'      | [:]
        [1,2,3,4,5]    | [:]
        [1,2,3,4,5,6]  | ['test':[new val.Result('should be no longer than 5',
                                                 'TOO_LONG')]]
    }

    def 'unless evaluates bodyCheck if testCheck does not pass'() {
        given:
        Check check = v.unless(v.isInstanceOf(Collection, key:'test'),
                v.hasSizeLte(5, key:'test'))

        expect:
        check(input, ctx) == ResultMap.from(results)

        where:
        input          | results
        null           | [:]
        '12345'        | [:]
        '1234567'      | ['test':[new val.Result('should be no longer than 5',
                                                 'TOO_LONG')]]
        [1,2,3,4,5,6]  | [:]
    }

    def 'not allows for inverting the behavior of a check'() {
        given:
        Check check = v.not(v.isInstanceOf(String, key:'test'))

        expect:
        check(input, ctx) == ResultMap.from(expected)

        where:
        input     | expected
        1         | [:]
        []        | [:]
        '1'       | [not: [
                new Result('condition satisfied which should not have been',
                           'NEGATED_CHECK')]]
    }

}
