package val

import spock.lang.Specification

class ResultNonNullSpecification extends Specification {

    def 'NPE is thrown if either message or code is null'() {
        when:
        new Result(message, code)

        then:
        thrown(NullPointerException)

        where:
        message  | code
        null     | 'foo'
        'foo'    | null
        null     | null
    }

}

class ResultValueObjectSpecification extends Specification {

    def 'equals compares by values'() {
        expect:
        (left == right) == expected

        where:
        left                 | right                || expected
        new Result('a','a')  | new Result('a','a')  || true
        new Result('a','b')  | new Result('a','a')  || false
    }

    def 'hash is calculated by values'() {
        expect:
        (left.hashCode() == right.hashCode()) == expected

        where:
        left                 | right                || expected
        new Result('a','a')  | new Result('a','a')  || true
        new Result('a','b')  | new Result('a','a')  || false
    }


    def 'toString returns $code: $message'() {
        expect:
        input.toString() == expected

        where:
        input                  || expected
        new Result('a', 'b')   || 'b: a'
    }
}
