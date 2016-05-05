suite = {
    "mxversion": "5.6.6",
    "name": "jrubytruffle",
    "defaultLicense": "EPL",

    "imports": {
        "suites": [
            {
                "name": "truffle",
                "version": "9c16d110f3aa63a17e3e2cf1b857e039c507078a",
                "urls": [
                    {"url": "https://github.com/graalvm/truffle.git",
                        "kind": "git"},
                    {"url": "https://curio.ssw.jku.at/nexus/content/repositories/snapshots", "kind": "binary"},
                ]
            },
        ],
    },
    "libraries": {
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
        "jruby-ruby": {
            "subDir": "lib/ruby",
            "class": "MavenProject",
            "build": "true",
            "prefix": "lib/ruby/",
            "dependencies": [
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DEBUG",
            ],
        },
        "jruby-lib-jni": {
            "subDir": "lib/jni",
            "class": "MavenProject",
            "prefix": "lib/jni/",
            "dependencies": [
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DEBUG",
            ],
        },
    },
    "licenses": {
        "EPL": {
            "name": "EPL",
            "url": "https://opensource.org/licenses/EPL-1.0",
        },
    },
    "distributions": {
        "RUBY": {
            "mainClass": "org.jruby.Main",
            "dependencies": [
                "RUBY_COMPLETE",
                "RUBY_TRUFFLE"
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
        "RUBY-ZIP": {
            "dependencies": [
                "jruby-ruby",
                "jruby-lib-jni",
            ],
            "exclude": [
                "truffle:JLINE",
            ],
            "distDependencies": [
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_DEBUG",
            ],
            "description": "JRuby+Truffle Native Libs",
            "license": "EPL"
        },
    },
}
