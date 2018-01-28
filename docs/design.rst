#######
Design
#######

Problem Definition
-------------------
In designing a validation libray the first question is what the library
should do (and whether that warrants a library instead of ad-hoc code).
Useful validation consists of two basic concerns in its simplest form:

* evaluating whether input is valid
* providing feedback if input is invalid

It is because of the combination of these two aspects that providing a
library is worthwhile. Asserting the validity of input alone could be
done through standard predicate calls and kept organized with
appropriate discipline. When that is combined with the need to collect
useful feedback, however, it is likely to result in code which devolves
as the validation needs increase. This library, therefore, essentially
provides a specialized form of predicate (a Check) which can be used to collect
such feedback rather than simple boolean values. Beyond that, it provides
the facilities to compose more complex rules against structures rather than
independent values, and some sugary bits to make some rules read more elegantly.
