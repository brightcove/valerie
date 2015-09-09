[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

valerie
===
> call on me

A lightweight unassuming validation library for whatever

## Why?
Most validation libraries are oriented around making sure your business objects are in a desirable state,
but when it comes time to validate something that is less well defined the structure may prove to be too rigid.

Most validation frameworks are oriented around validating _objects_ while valerie instead helps to validate _input_.
While objects represent some form of expected structure (and provide value in doing so), input is...whatever a
client tries to send you.

If you want submitted input to be different from the defined business model
([Command Model](http://martinfowler.com/bliki/CQRS.html)) or you just want to provide better feedback for
whatever kind of garbage someone may throw at you, then a lot of existing frameworks either get in the way
or lead to so much of a mess that you lose any structural or declarative benefit.

## How?
Validation is a good fit for the functional programming paradigm so that is the basis for this library
(it will initially be entirely functional for conceptual clarity). Descriptive validation rules can
be created by creating appropriately named functions (normally out of more primitive functions).

This will initially be written in Groovy though other languages could be supported fairly easily.

User oriented information should be created, possibly in a GitHub wiki.