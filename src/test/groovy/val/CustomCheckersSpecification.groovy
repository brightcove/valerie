package val

import spock.lang.Specification

class CustomCheckersSpecification extends Specification {
    EvalContext ctx = new EvalContext()
    ResultMap testResult = ResultMap.from(['test': [new Result('successful',
            'WOOHOO')]])

    class MyCheckLibrary extends Checkers {
        Check myTestChecker() {
            { String a, ctx -> testResult }
        }

        Check isRequiredString(Map mold=[:]) {
            isNotNull(mold) & isInstanceOf(mold, String) & hasSizeGte(mold, 1)
        }
    }

    def 'a custom checker can be used by extending Checkers'() {
        expect:
        new Idator().using(new MyCheckLibrary()){
            define myTestChecker()}('input', ctx) ==
                testResult
    }

    def 'custom registered checkers behave like standard checkers'() {
        expect:
        new Idator().using(new MyCheckLibrary()){
            subDefine child: { isRequiredString() }
            define name: { isRequiredString() }
        }(input, ctx) == ResultMap.from(expected)

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
