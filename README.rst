.. image:: https://travis-ci.org/brightcove/valerie.svg?branch=master
   :target: https://travis-ci.org/brightcove/valerie
   :alt: Build Status

.. image:: http://img.shields.io/:license-apache-blue.svg
   :target: http://www.apache.org/licenses/LICENSE-2.0.html
   :alt: License

.. image:: https://api.bintray.com/packages/brightcove/valerie/valerie/images/download.svg
   :target: https://bintray.com/brightcove/valerie/valerie/_latestVersion
   :alt: Download

#######
Valerie
#######

.. epigraph::

   Call on me.

   -- Steve Winwood

A first class validation library for the JVM.

Validate your objects using a DSL which allows defining your rules
in a structure which mirrors your inputs and objects.

********
Metadata
********

Status
	Active
Type
	Library
Versioning
	Semantic
Documentation
	`Read the Docs <https://valerie.readthedocs.io/en/latest/>`_
Maintainers
	`CODEOWNERS <./CODEOWNERS>`_
Contact/Questions/Issues
	`GitHub Issues <https://www.github.com/brightcove/valerie/issues>`_
Contributions
	Contributions are welcome. A section on developing will be added to
	the documentation, until that occurs file an issue prior to any
	non-trivial pull request to verify the suitability of envisioned
        changes.

************
Installation
************

Valerie is available for download from jCenter. It is presently not
published to Maven Central, but can be so if desired (file an
Issue). The artifacts can be manually downloaded from jCenter/BinTray
by clicking on the Download link/badge at
the top of this README.

Gradle
======

Make sure jCenter is configured as a repository:

.. code-block:: gradle

   repositories {
     jcenter()
   }

Declare a compile time dependency on Valerie:

.. code-block:: gradle

   dependencies {
     compile 'com.brightcove:valerie:latest.release'
   }

``latest.release`` will automatically be resolved by Gradle but using
some form of fixed version or locking is recommended: the latest
released version can be quickly retrieved from the Download badge on
the top of this README.
