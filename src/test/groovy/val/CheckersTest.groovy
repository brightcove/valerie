package val

import spock.lang.Specification
import spock.lang.Unroll

class CheckersTest extends Specification {

    def v = new Checkers()

    def 'fail creates ResultMap from arguments'() {
        expect:
        v.fail()('anything') == ResultMap.from('fail': [new Result('failed because you said so',
                Result.CODE_ILLEGAL_VALUE)])
        v.fail('foo', 'bar', 'FOO_BAR')('anything') == ResultMap.from('foo': [new Result('bar', 'FOO_BAR')])
    }

    @Unroll
    def 'hasMember checks to see whether member is present, only supporting maps right now'() {
        expect:
        v.hasMember(attribute, 'required')(input) == ResultMap.from(results)

        where:
        input            | attribute  | results
        [a:'test']       | 'a'        | [:]
        [a: null]        | 'a'        | [:]
        [:]              | 'a'        | [required:[new val.Result('required field a is not present',
                val.Result.CODE_REQUIRED_FIELD)]]
    }

    def 'hasOnlyFieldsIn will return errors for any fields that are not in provided set'() {
        expect:
        v.hasOnlyFieldsIn(['a','b','c'] as Set, null)(input) == ResultMap.from(results)

        where:
        input                 | results
        [a:1]                 | [:]
        [a:1,b:2,c:3]         | [:]
        [a:1,b:2,c:3,d:4,e:5] | [ d:[new val.Result('field is unknown', val.Result.CODE_ILLEGAL_FIELD)],
                                  e:[new val.Result('field is unknown', val.Result.CODE_ILLEGAL_FIELD)] ]
    }

    def 'hasOnlyFieldsIn will create child keys based on provided key'() {
        expect:
        v.hasOnlyFieldsIn(['a','b',3] as Set, 'parent')(input) == ResultMap.from(results)

        where:
        input                 | results
        [a:1,b:2,3:3,d:4,e:5] | ['parent.d':[new val.Result('field is unknown', val.Result.CODE_ILLEGAL_FIELD)],
                                 'parent.e':[new val.Result('field is unknown', val.Result.CODE_ILLEGAL_FIELD)] ]
    }

    class SampleClass {
        def a, b, c
    }
    def 'hasOnlyFieldsIn will treat the declared fields of a provided Class as the allowed Set'() {
        expect:
        v.hasOnlyFieldsIn(SampleClass, null)(input) == ResultMap.from(results)

        where:
        input                 | results
        [a:1]                 | [:]
        [a:1,b:2,c:3]         | [:]
        [a:1,b:2,c:3,d:4,e:5] | [ d:[new val.Result('field is unknown', val.Result.CODE_ILLEGAL_FIELD)],
                                  e:[new val.Result('field is unknown', val.Result.CODE_ILLEGAL_FIELD)] ]
    }

    def 'hasSizeGte checks for inclusive minimum size'() {
        expect:
        v.hasSizeGte(1, 'value')(input) == ResultMap.from(results)

        where:
        input        | results
        null         | [:]
        '1'          | [:]
        [1]          | [:]
        [a:1]        | [:]
        ''           | [value:[new val.Result('should be on least 1 long', val.Result.CODE_TOO_SHORT)]]
        []           | [value:[new val.Result('should be on least 1 long', val.Result.CODE_TOO_SHORT)]]
        [:]          | [value:[new val.Result('should be on least 1 long', val.Result.CODE_TOO_SHORT)]]
    }

    @Unroll
    def 'hasSizeLte checks for inclusive maximum size'() {
        expect:
        v.hasSizeLte(3, 'value')(input) == ResultMap.from(results)

        where:
        input              | results
        null               | [:]
        '123'              | [:]
        [1,2,3]            | [:]
        [a:1,b:2,c:3]      | [:]
        '1234'             | [value:[new val.Result('should be no longer than 3', val.Result.CODE_TOO_LONG)]]
        [1,2,3,4]          | [value:[new val.Result('should be no longer than 3', val.Result.CODE_TOO_LONG)]]
        [a:1,b:2,c:3,d:4]  | [value:[new val.Result('should be no longer than 3', val.Result.CODE_TOO_LONG)]]
    }

    def 'hasValueGte checks for inclusive minimum value'() {
        expect:
        v.hasValueGte(min,'val')(input) == ResultMap.from(results)

        where:
        min    | input   | results
        5      | 6       | [:]
        5      | 5       | [:]
        5      | 4       | [val: [new val.Result('should not be less than 5', val.Result.CODE_ILLEGAL_VALUE)]]
        'G'    | 'G'     | [:]
        'G'    | 'g'     | [:]
        'G'    | 'a'     | [:]
        'G'    | 'A'     | [val: [new val.Result('should not be less than G', val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'hasValueLte checks for inclusive maximum value'() {
        expect:
        v.hasValueLte(max,'val', 'should be less than max')(input) == ResultMap.from(results)

        where:
        max        | input          | results
        5          | 4              | [:]
        5          | 5              | [:]
        5          | 6              | [val: [new val.Result('should be less than max', val.Result.CODE_ILLEGAL_VALUE)]]
        new Date() | new Date() - 1 | [:]
        new Date() | new Date() + 1 | [val: [new val.Result('should be less than max', val.Result.CODE_ILLEGAL_VALUE)]]
    }

    def 'isInstanceOf checks whether input extends type'() {
        expect:
        v.isInstanceOf(type, 'type')(input) == ResultMap.from(results)

        where:
        input       | type         | results
        null        | String       | [:]
        'test'      | String       | [:]
        "${'test'}" | String       | [type:[new val.Result('is not of type String', 'ILLEGAL_VALUE')]]
        'test'      | Integer      | [type:[new val.Result('is not of type Integer', 'ILLEGAL_VALUE')]]
        []          | Iterable     | [:]
        []          | ArrayList    | [:]
        []          | Map          | [type:[new val.Result('is not of type Map', 'ILLEGAL_VALUE')]]
    }

    def 'isOneOf checks whether input is a member of the set of permitted values'() {
        expect:
        v.isOneOf(allowed,'value')(input) == ResultMap.from(results)

        where:
        input         | allowed   | results
        'any'         | ['any']   | [:]
        []            | ['any']   | [value:[new val.Result('is not one of allowed values: [any]', 'ILLEGAL_VALUE')]]
        [:]           | [[:]]     | [:]
        []            | [[]]      | [:]
        []            | [['test']]| [value:[new val.Result('is not one of allowed values: [[test]]', 'ILLEGAL_VALUE')]]
        'not'         | ['any']   | [value:[new val.Result('is not one of allowed values: [any]', 'ILLEGAL_VALUE')]]
        1             | [2,4]     | [value:[new val.Result('is not one of allowed values: [2, 4]', 'ILLEGAL_VALUE')]]
        2             | [2,4]     | [:]
        null          | [null]    | [:]
    }

    enum Test {A, B, C}  ///could use a standard enum but I don't know any off the top of my head
    def 'isOneOf checks if input is valid value for enum'() {
        given:
        expect:
        v.isOneOf(Test, 'value')(input) == ResultMap.from(results)

        where:
        input        | results
        Test.A       | [:]
        'C'          | [:]
        null         | [:]
        'D'          | [value:[new val.Result('should be one of [A, B, C]', 'ILLEGAL_VALUE')]]
        ''           | [:]
    }

    def 'isNotNull ensures input is not null'() {
        expect:
        v.isNotNull('required')(input) == ResultMap.from(results)

        where:
        input     | results
        'valid'   | [:]
        ''        | [:]
        0         | [:]
        false     | [:]
        null      | ['required':[new val.Result('required field cannot be null', 'REQUIRED_FIELD')]]
    }

    def 'isNull ensures input is null'() {
        expect:
        v.isNull('required')(input) == ResultMap.from(results)

        where:
        input     | results
        null      | [:]
        'bad'     | ['required':[new val.Result('field must be null', 'ILLEGAL_VALUE')]]
        ''        | ['required':[new val.Result('field must be null', 'ILLEGAL_VALUE')]]
        0         | ['required':[new val.Result('field must be null', 'ILLEGAL_VALUE')]]
        [:]       | ['required':[new val.Result('field must be null', 'ILLEGAL_VALUE')]]
    }

    def 'matchesRe checks whether value matches provided regular expression'() {
        expect:
        v.matchesRe(pattern,'value')(input) == ResultMap.from(results)

        where:
        input        | pattern   | results
        null         | /\d+/     | [:]
        '21'         | /\d+/     | [:]
        ''           | /\d+/     | [value:[new val.Result('does not match required pattern', 'ILLEGAL_VALUE')]]
        'a'          | /\d+/     | [value:[new val.Result('does not match required pattern', 'ILLEGAL_VALUE')]]
        'yes'        | /y\w+/    | [:]
        'other'      | /y\w+/    | [value:[new val.Result('does not match required pattern', 'ILLEGAL_VALUE')]]
        'oyeah'      | /y\w+/    | [value:[new val.Result('does not match required pattern', 'ILLEGAL_VALUE')]]
        123          | /\d+/     | [:]
        123          | /y\w+/    | [value:[new val.Result('does not match required pattern', 'ILLEGAL_VALUE')]]
        ['1']        | /\d+/     | [value:[new val.Result('does not match required pattern', 'ILLEGAL_VALUE')]]
        ['1']        | /.*\d.*/  | [:]
        [b:'12']     | /.*b.*/   | [:]
        ['b']        | /\d+/     | [value:[new val.Result('does not match required pattern', 'ILLEGAL_VALUE')]]
        ['yes']      | /y\w+/    | [value:[new val.Result('does not match required pattern', 'ILLEGAL_VALUE')]]
    }

    def 'pass returns passed'() {
        expect:
        v.pass()('anything') == ResultMap.passed()
    }

    def 'satisfies(test,key,message,code) returns ResultMap based on arguments if test(value) fails, else a passing ResultMap'() {
        expect:
        v.satisfies({it}, 'invalid', 'uh oh', 'BAD_VALUE')(input) == ResultMap.from(expected)

        where:
        input     | expected
        true      | [:]
        false     | [invalid: [new val.Result('uh oh', 'BAD_VALUE')]]
    }

    def 'satisfies(test,key,message,code) allows late bound GStrings to include info input in the result'() {
        expect:
        v.satisfies({it}, 'invalid', "$input is not truthy", 'BAD_VALUE')(input) == ResultMap.from(expected)

        where:
        input     | expected
        true      | [:]
        false     | [invalid: [new val.Result('false is not truthy', 'BAD_VALUE')]]
        []        | [invalid: [new val.Result('[] is not truthy', 'BAD_VALUE')]]
    }



    def 'satisfies(test, onFail) returns onFail result if test returns non-truthy, else a passing ResultMap'() {
        expect:
        v.satisfies({it},
                {input -> ResultMap.from([fail:[new val.Result('fail', 'fail')]])})(input) == ResultMap.from(expected)

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
        def check = v.all(
                v.isNotNull('test'),
                v.isInstanceOf(String, 'test'),
                v.isOneOf(['a','b'], 'test') )

        expect:
        check(input) == ResultMap.from(results)

        where:
        input    | results
        null     | [test:[new val.Result('required field cannot be null', 'REQUIRED_FIELD'),
                          new val.Result('is not one of allowed values: [a, b]', 'ILLEGAL_VALUE')]]
        1        | [test:[new val.Result('is not of type String', 'ILLEGAL_VALUE'),
                          new val.Result('is not one of allowed values: [a, b]', 'ILLEGAL_VALUE')]]
        'a'      | [:]
        'c'      | [test:[new val.Result('is not one of allowed values: [a, b]', 'ILLEGAL_VALUE')]]
    }

    def 'and provides short circuiting behavior standard to most logical ands'() {
        given:
        def check = v.and(
                v.isNotNull('test'),
                v.isInstanceOf(String, 'test'),
                v.isOneOf(['a','b'], 'test') )

        expect:
        check(input) == ResultMap.from(results)

        where:
        input    | results
        null     | [test:[new val.Result('required field cannot be null', 'REQUIRED_FIELD')]]
        1        | [test:[new val.Result('is not of type String', 'ILLEGAL_VALUE')]]
        'a'      | [:]
        'c'      | [test:[new val.Result('is not one of allowed values: [a, b]', 'ILLEGAL_VALUE')]]
    }

    def 'or provides short circuiting behavior standard to most logical ors or returns aggregated results'() {
        given:
        def check = v.or(
                v.isInstanceOf(String, 'test'),
                v.isInstanceOf(Integer, 'test'))

        expect:
        check(input) == ResultMap.from(results)

        where:
        input    | results
        null     | [:]
        1        | [:]
        'a'      | [:]
        []       | [test:[new val.Result('is not of type Integer', 'ILLEGAL_VALUE')]]
    }

    //
    // OTHER HIGHER ORDER FUNCTIONS
    //
    def 'when evaluates bodyCheck if testCheck passes'() {
        given:
        def check = v.when(v.isInstanceOf(Collection, 'test'), v.hasSizeLte(5, 'test'))

        expect:
        check(input) == ResultMap.from(results)

        where:
        input          | results
        null           | [:]
        '1234567'      | [:]
        [1,2,3,4,5]    | [:]
        [1,2,3,4,5,6]  | ['test':[new val.Result('should be no longer than 5', 'TOO_LONG')]]
    }

    def 'unless evaluates bodyCheck if testCheck does not pass'() {
        given:
        def check = v.unless(v.isInstanceOf(Collection, 'test'), v.hasSizeLte(5, 'test'))

        expect:
        check(input) == ResultMap.from(results)

        where:
        input          | results
        null           | [:]
        '12345'        | [:]
        '1234567'      | ['test':[new val.Result('should be no longer than 5', 'TOO_LONG')]]
        [1,2,3,4,5,6]  | [:]
    }

    def 'not allows for inverting the behavior of a check'() {
        expect:
        v.not(v.isInstanceOf(String, 'test'))(input) == ResultMap.from(expected)

        where:
        input     | expected
        1         | [:]
        []        | [:]
        '1'       | [not: [new Result('condition satisfied which should not have been', 'NEGATED_CHECK')]]

    }

}
