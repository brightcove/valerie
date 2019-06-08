[![Build Status](https://travis-ci.org/brightcove/valerie.svg?branch=master)]
(https://travis-ci.org/brightcove/valerie)
[![License](http://img.shields.io/:license-apache-blue.svg)]
(http://www.apache.org/licenses/LICENSE-2.0.html)
[![Download](
https://api.bintray.com/packages/brightcove/valerie/valerie/images/download.svg
) ]
(https://bintray.com/brightcove/valerie/valerie/_latestVersion)

# valerie

> (don't) call on me (anymore) - Steve Winwood

A first class validation library for the JVM

## Abandoned

It has been decided to halt development on Valerie. In working
with this project it has become clear that core valuable pieces
it provides are:

- Combinable, expressive validation feedback.
- Applying composable validation while traversing a tree/graph.

The concluding line of thought is that this library should be ported
to Java and be focused on those pieces while leaving the collection
of predicates/matchers to other libraries. If that is done,
it is not yet clear whether there would be enough substance left
to justify having a library rather than capturing it in project
local code.

This is ultimately being abandoned because there has now been
a sustained period during which there has been ideas for ways
to improve the library, but no driving need to do so.

### It's Done its Job

Valerie has been in use in the system for which it was created for
several years without the need for enhancement. The initial release
provides flexible, albeit often awkward, building blocks; while
there has been a background desire to provide some constructs which
would allow for more eloquent definitions, it's never been strictly
necessary and has never gathered enough mass to be given priority.
Aside from the notable deficiency of lack of documentation, it's
been extensible enough to evolve with the system without issue.

### It's Not Doing Other Jobs

There have been multiple projects for which validation has
been implemented and for which there was a thought that Valerie
would be pulled in (after removal of Groovy as a runtime dependency).
In all of these cases, however, that never ended up happening;
there was a repeated pattern of waiting for the validation requirements
to grow enough in complexity, but such a tipping point was never reached.
This leads to the conclusion that the confluence of forces that led to
the inception of Valerie are uncommon. The key factor was likely
non-trivial validation requirements which were being addressed by an
implementation which was not conducive to evolution and therefore
entropy dragged it into a tangle.
Most services tend to have fairly simple validation requirements, and
those that have or accrete more complex needs can apply some
well-established practices to keep the validation in order without
requiring the assistance of an additional library.

### It's on the Path to Deprecation

It's likely that the reference consumer of Valerie will shift to an
alternative moving forward. A runtime dependency on Groovy is likely
to be avoided and for the aforementioned reasons porting
Valerie does not seem worthwhile, though some of the approaches
herein may be implemented locally as required. The target service
has a fair amount of tooling which is driven by OpenAPI and
therefore a solution which is driven by JSON Schema definitions
within an OpenAPI file is likely.

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
