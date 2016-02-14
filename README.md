[![Build Status](https://travis-ci.org/brightcove/valerie.svg?branch=master)]
(https://travis-ci.org/brightcove/valerie)
[![License](http://img.shields.io/:license-apache-blue.svg)]
(http://www.apache.org/licenses/LICENSE-2.0.html)
[![Download](
https://api.bintray.com/packages/brightcove/valerie/valerie/images/download.svg
) ]
(https://bintray.com/brightcove/valerie/valerie/_latestVersion)

valerie
===
> call on me

A first class validation library for the JVM

First Class Validation
---
Most validation libraries are defined in terms of an established
business model and are therefore second class in relation to that
model. This leads to validation being conceptually _behind_ the
binding of input to the model, rather than in between the input and
the model; to put it another way, most validation libraries are
focused on validating *objects* rather than less constrained
*input*. By providing first class validation that is not bound to a
defined model, Valerie allows for a wider range of validation rules
for an accordingly wide range of possible inputs.

The more common second class approach mentioned above (defining
validation in terms of your model) is a very good one as it provides
many conveniences, is generally DRY'er, and can highlight the role of
the model in the code.  There are times, however, when this approach
becomes overly-restrictive. Some examples may include:
 * providing richer feedback to less defined or more widely varying user input
 * allowing the model to evolve cleanly while migrating the API to that model
 * separating the read/query model from the write/command model (CQRS) or validating partial representations/patches for domain objects

### Installation
Valerie is available for download from jCenter. It is presently not
published to Maven Central, but can be so if desired (file an
Issue). The artifacts can be manually downloaded from jCenter/BinTray
by clicking on the Download link/badge at
the top of this README.

#### Gradle
Make sure jCenter is configured as a repository:
```gradle
repositories {
  jcenter()
}
```
Declare a compile time dependency on Valerie:
```gradle
dependencies {
  compile 'com.brightcove:valerie:latest.release'
}
```
`latest.release` will automatically be resolved by Gradle but using
some form of fixed version or locking is recommended: the latest
released version can be quickly retrieved from the Download badge on
the top of this REAMDE

### Horses for Courses
The separation of validation is likely to lead to more work and a more
fragmented domain model, but in cases like those above, second class
validation may start to get in the way rather than offering help. The
ensuing implementation difficulties and/or divergent changes can lead
to a loss of code structure and readibility. Valerie provides a
decoupled alternative which allows for consistent and declarative
code.

The goal of this project is not to improve upon or replace existing
validation libraries but to solve a slightly different problem. If
defining your validation in terms of your model works, then you should
do so: if you find your model getting blurry or polluted due to
validation concerns that don't fit in cleanly, or the general
validation logic has devolved into a tangle due to the restrictions of
your validation framework, then switching to first class validation
may make sense.
