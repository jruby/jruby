# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

def mavenLib(mavenDep, sha1, sourceSha1):
    groupId, artifactId, version = mavenDep.split(':')
    args = (groupId.replace('.', '/'), artifactId, version, artifactId, version)
    base = "https://search.maven.org/remotecontent?filepath=%s/%s/%s/%s-%s" % args
    url = base + ".jar"
    sourceUrl = base + '-sources.jar'
    return {
        "urls": [ url ],
        "sha1": sha1,
        "sourceUrls": [ sourceUrl ],
        "sourceSha1": sourceSha1,
        "maven": {
            "groupId": groupId,
            "artifactId": artifactId,
            "version": version,
        },
        "license": "EPL", # fake
    }

suite = {
    "mxversion": "5.51.0",
    "name": "jrubytruffle",
    "defaultLicense": "EPL",

    "imports": {
        "suites": [
            {
                "name": "truffle",
                # Must be the same as in truffle/pom.rb (except for the -SNAPSHOT part only in pom.rb, and there we can use a release name)
                "version": "af21c6632dbc4e8c48cd25d0b7d623d379c888e6",
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

        "ANTLR4_RUNTIME": mavenLib(
            "org.antlr:antlr4-runtime:4.5.1-1",
            "66144204f9d6d7d3f3f775622c2dd7e9bd511d97",
            "e0057705e96c77719e7d49cb64dc014d023e7a9b"),

        "SNAKEYAML": mavenLib(
            "org.yaml:snakeyaml:1.14",
            "c2df91929ed06a25001939929bff5120e0ea3fd4",
            "4c6bcedc3efa772a5ae1c2fd01efee8e4d15edac"),
    },

    "projects": {

        # ------------- Projects -------------

        "jruby-core": {
            "class": "JRubyCoreMavenProject",
            "sourceDirs": [ "core/src/main/java" ],
            "watch": [ "core/src" ],
            "jar": "lib/jruby.jar",
        },

        "jruby-truffle": {
            "dir": "truffle/src/main",
            "sourceDirs": [ "java" ],
            "dependencies": [
                "jruby-core",
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DEBUG",
                "ANTLR4_RUNTIME",
                "SNAKEYAML",
            ],
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "javaCompliance": "1.8",
            "workingSets": "JRubyTruffle",
            "checkPackagePrefix": "false",
        },

        "jruby-truffle-ruby": {
            "class": "ArchiveProject",
            "outputDir": "truffle/src/main/ruby",
            "prefix": "jruby-truffle",
        },

        "jruby-truffle-test": {
            "dir": "truffle/src/test",
            "sourceDirs": ["java"],
            "dependencies": [
                "jruby-truffle",
                "truffle:TRUFFLE_TCK",
                "mx:JUNIT",
            ],
            "javaCompliance": "1.8",
            "checkPackagePrefix": "false",
        },

        "jruby-truffle-ruby-test": {
            "class": "ArchiveProject",
            "outputDir": "truffle/src/test/ruby",
            "prefix": "src/test/ruby",
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
            "overlaps": [
                "RUBY",
            ],
            "description": "JRuby+Truffle Native Libs",
            "license": "EPL"
        },

        "RUBY-TEST": {
            "dependencies": [
                "jruby-truffle-test",
                "jruby-truffle-ruby-test",
            ],
            "exclude" : [
                "mx:HAMCREST",
                "mx:JUNIT"
            ],
            "distDependencies": [
                "RUBY",
                "truffle:TRUFFLE_TCK"
            ],
            "license" : "EPL",
        },
    },
}
