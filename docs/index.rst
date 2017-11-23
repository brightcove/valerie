########
Valerie
########

.. epigraph::
   Call on me

   -- Steve Winwood

A first class validation DSL for the JVM.

.. toctree::
   :hidden:
   :maxdepth: 1

   api

.. contents::
   :local:
   :depth: 2

Overview
=========	     

Valerie is a DSL which allows conventional validation logic to be
succintly defined using canned or created rules, while allowing more
complex or unique rules to be expressed in a way that is cleanly
integrated into the same validation structure.

First Class
------------
Many validation libraries are defined in terms of an established
business model and are therefore second class in relation to that
model. This leads to validation being conceptually _behind_ the
binding of input to the model, rather than in between the input and
the model; to put it another way, most validation libraries are
focused on validating *objects* rather than less constrained
*input*. By providing first class validation that is not bound to a
defined model, Valerie allows for a wider range of validation rules
for an accordingly wide range of possible inputs.

.. TODO: Diagram this
   
Comparison With Alternatives
-----------------------------
The comparisons below are representative rather than comprehensive,
suggestions/requests for further comparisons are welcome. There is no
intentention to claim that Valerie is *superior* to alternatives,
each is likely to best satisfy a certain set of needs. The comparisons
here should provide some guidance in determining whether the design of
Valerie makes sense given the forces applied to a particular scenario.

JSR-303/javax.validation
^^^^^^^^^^^^^^^^^^^^^^^^^
The approach taken by standard Java bean validation is a good one.
It provides many conveniences, is generally DRY'er than independent
validation, and can highlight the role of the model in the code.
There are times, however, when this approach
becomes overly-restrictive. Some examples may include:

* providing richer feedback for a wider range of user input
* allowing the model to evolve ahead of the exposed API
* separating the read/query model from the write/command model (CQRS)
* validating partial representations/patches for domain objects

JSON Schema
^^^^^^^^^^^^
Schema formats are very appropriate for validating against standard
specifications. A fully declarative system, however, often proves
too rigid for more specialized business requirements. More complex
validation rules often pass the point of diminishing returns
where the provided validation logic becomes increasingly fragmented
as the schema framework is stretched far past its original intent.

Quick Start
============
Valerie is available for download from jCenter. It is presently not
published to Maven Central, but can be so if desired (file an Issue). 

With Gradle
------------------

Make sure jCenter is configured as a repository::

  repositories {
    jcenter()
  }

Declare a compile time dependency on Valerie::

  dependencies {
    compile 'com.brightcove:valerie:latest.release'
  }

`latest.release` will automatically be resolved by Gradle but using
some form of fixed version or locking is recommended.

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`
