# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

def mavenLib(mavenDep, sha1):
    groupId, artifactId, version = mavenDep.split(':')
    args = (groupId.replace('.', '/'), artifactId, version, artifactId, version)
    url = "https://search.maven.org/remotecontent?filepath=%s/%s/%s/%s-%s.jar" % args
    return {
        "urls": [ url ],
        "sha1": sha1,
        "maven": {
            "groupId": groupId,
            "artifactId": artifactId,
            "version": version,
        },
        "license": "EPL", # fake
    }

suite = {
    "mxversion": "5.36.1",
    "name": "jrubytruffle",
    "defaultLicense": "EPL",

    "imports": {
        "suites": [
            {
                "name": "truffle",
                "version": "5e141bd0895239307edb5a4c64eaccb7d37409ef",
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

        "ANTLR4_MAIN": mavenLib(
            "org.antlr:antlr4:4.5.1-1",
            "a8867c83a73791cf30e30de4cf5d0c9a5f0dfdab"),

        "ANTLR4_RUNTIME": mavenLib(
            "org.antlr:antlr4-runtime:4.5.1-1",
            "66144204f9d6d7d3f3f775622c2dd7e9bd511d97"),

        "SNAKEYAML": mavenLib(
            "org.yaml:snakeyaml:1.14",
            "c2df91929ed06a25001939929bff5120e0ea3fd4"),
    },

    "projects": {

        # ------------- Projects -------------

        "jruby-core": {
            "class": "JRubyCoreMavenProject",
            "sourceDirs": [ "core/src/main/java" ],
            "watch": [ "core/src" ],
            "jar": "lib/jruby.jar",
        },

        "jruby-antlr": {
            "class": "AntlrProject",
            "sourceDir": "truffle/src/main/antlr4",
            "outputDir": "truffle/target/generated-sources/antlr4",
            "grammars": [ "org/jruby/truffle/core/format/pack/Pack.g4" ],
            "dependencies": [ "ANTLR4_RUNTIME" ],
        },

        "jruby-truffle": {
            "dir": "truffle",
            "sourceDirs": [
                "src/main/java",
                "target/generated-sources/antlr4",
            ],
            "dependencies": [
                "jruby-core",
                "jruby-antlr",
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DEBUG",
                "ANTLR4_RUNTIME",
                "SNAKEYAML",
            ],
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "javaCompliance": "1.8",
            "workingSets": "JRubyTruffle",
        },

        "jruby-truffle-ruby": {
            "class": "ArchiveProject",
            "outputDir": "truffle/src/main/ruby",
            "prefix": "jruby-truffle",
        },

        # Depends on jruby-core extracting jni libs in lib/jni
        "jruby-lib-jni": {
            "class": "ArchiveProject",
            "outputDir": "lib/jni",
            "prefix": "lib/jni",
            "dependencies": [ "jruby-core" ],
        },

        # Depends on jruby-core installing gems in lib/ruby
        "jruby-lib-ruby": {
            "class": "ArchiveProject",
            "outputDir": "lib/ruby",
            "prefix": "lib/ruby",
            "dependencies": [ "jruby-core" ],
        },

        "jruby-licences": {
            "class": "LicensesProject",
            "outputDir": "",
            "prefix": "",
        },
    },

    "distributions": {

        # ------------- Distributions -------------

        "RUBY": {
            "mainClass": "org.jruby.Main",
            "dependencies": [
                "jruby-core",
                "jruby-truffle",
                "jruby-truffle-ruby",
            ],
            "exclude": [
                "truffle:JLINE",
            ],
            "distDependencies": [
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
