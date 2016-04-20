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

    "jruby-truffle" : {
      "subDir" : "",
      "sourceDirs" : ["src/main/java"],
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
        "jruby-truffle",
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
