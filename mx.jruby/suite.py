suite = {
  "mxversion" : "5.6.6",
  "name" : "jrubytruffle",
  "defaultLicense" : "GPLv2-CPE", # FIXME

  "imports" : {
    "suites": [
            {
                "name": "truffle",
                "version": "551e8475af2fc8769bc3ead07c9156fe0ccbe338",
                "urls": [
                    {"url": "https://github.com/graalvm/truffle.git",
                     "kind": "git"},
                    { "url" : "https://curio.ssw.jku.at/nexus/content/repositories/snapshots", "kind" : "binary" },
                ]
            },
    ],
  },

  "projects" : {

    # ------------- Projects -------------

    # truffle/target/generated-sources/org/jruby/truffle/runtime/layouts
    # NOT WORKING since mutual dependencies :/
    # "org/jruby/truffle/runtime/layouts" : {
    #   "subDir": "truffle/target/generated-sources",
    #   "sourceDirs": [""],
    #   "dependencies": [
    #     "JRUBY_CORE",
    #     "truffle:TRUFFLE_API",
    #   ],
    #   "javaCompliance" : "1.7",
    # },

    # truffle/target/generated-sources/antlr4
    "generated-sources/antlr4" : {
      "subDir": "truffle/target",
      "sourceDirs": ["."],
      "dependencies": [
      ],
      "javaCompliance" : "1.7",
    },

    # truffle/src/main/java
    "truffle/src/main/java" : {
      "subDir" : "",
      "sourceDirs" : ["."],
      "dependencies": [
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_DEBUG",
      ],
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "javaCompliance" : "1.7",
      "workingSets" : "JRubyTruffle",
    },

  },
  "licenses" : {
    "GPLv2" : {
      "name" : "GPLv2",
      "url" : "http://opensource.org/licenses/GPL-2.0",
    },
  },
  "distributions" : {

    # ------------- Distributions -------------

    "JRUBY_TRUFFLE" : {
      "javaCompliance" : "1.7",
      "dependencies" : [
        "truffle/src/main/java",
      ],
      "exclude" : [
        "truffle:JLINE",
      ],
      "distDependencies" : [
      ],
      "description" : "JRuby+Truffle",
    },

  },
}
