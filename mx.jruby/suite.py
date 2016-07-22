# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

suite = {
    "mxversion": "5.31.4",
    "name": "jrubytruffle",
    "defaultLicense": "EPL",

    "imports": {
        "suites": [
            {
                "name": "truffle",
                "version": "47033f56665100fd5f7cbafd96d6c3112329f517",
                "urls": [
                    {"url": "https://github.com/graalvm/truffle.git", "kind": "git"},
                    {"url": "https://curio.ssw.jku.at/nexus/content/repositories/snapshots", "kind": "binary"},
                ]
            },
        ],
    },

    "licenses": {
        "EPL": {
            "name": "EPL",
            "url": "https://opensource.org/licenses/EPL-1.0",
        },
    },

    "libraries": {

        # ------------- Libraries -------------

        "RUBY_COMPLETE": {
            "path": "maven/jruby-complete/target/jruby-complete-graal-vm.jar",
            "sha1": "NOCHECK",
            "optional":"true",
            "license": "EPL"
        },

        "RUBY_TRUFFLE": {
            "path": "lib/jruby-truffle.jar",
            "sha1": "NOCHECK",
            "optional":"true",
            "license": "EPL"
        },
    },

    "projects": {

        # ------------- Projects -------------

        "jruby-maven": {
            "class": "MavenProject",
            "watch": ["core/src", "truffle/src"],
            "jar": "maven/jruby-complete/target/jruby-complete-graal-vm.jar",
            "dependencies": [
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DEBUG",
            ],
        },

        # Depends on jruby-maven extracting jni libs in lib/jni
        "jruby-lib-jni": {
            "class": "ArchiveProject",
            "prefix": "lib/jni",
        },

        "jruby-lib-ruby": {
            "class": "ArchiveProject",
            "prefix": "lib/ruby",
        },

        "jruby-licences": {
            "class": "LicensesProject",
            "prefix": ".",
        },
    },

    "distributions": {

        # ------------- Distributions -------------

        "RUBY": {
            "mainClass": "org.jruby.Main",
            "dependencies": [
                # "jruby-maven",
                "RUBY_COMPLETE",
                "RUBY_TRUFFLE",
            ],
            "exclude": [
                "truffle:JLINE",
            ],
            "distDependencies": [
                "RUBY-ZIP",
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DEBUG",
            ],
            "description": "JRuby+Truffle",
            "license": "EPL"
        },

        # Set of extra files to extract to run Ruby
        "RUBY-ZIP": {
            "native": True, # Not Java
            "relpath": True,
            "dependencies": [
                "jruby-lib-jni",
                "jruby-lib-ruby",
                "jruby-licences",
            ],
            "description": "JRuby+Truffle Native Libs",
            "license": "EPL"
        },
    },
}
