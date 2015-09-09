package val

import spock.lang.Specification

class ResultTest extends Specification {

    def 'equals compares by values'() {
        expect:
        (left == right) == expected

        where:
        left                | right               | expected
        new Result('a','a') | new Result('a','a') | true
    }
}
