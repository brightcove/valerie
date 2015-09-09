package val

import spock.lang.Specification

class IdatorTest extends Specification {

    def 'define loads an initial validation scope'() {
        given:
        def definedCheck = new val.Idator().define{isNotNull('test')}

        expect:
        definedCheck(null).asMap().containsKey('test')
        !(definedCheck('a').asMap().containsKey('test'))
    }

    def 'after a checker is registered it can be used in definition closures'() {
        given:
        def validator = new val.Idator()
        def testResult = val.ResultMap.from(['test':new val.Result('successful','WOOHOO')])

        when:
        validator.registerChecker('myTestChecker', { ->
            {input -> testResult }
        })

        then:
        validator.define{define myTestChecker()}('input') == testResult
    }

}
