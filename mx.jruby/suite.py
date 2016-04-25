suite = {
  "mxversion" : "5.6.6",
  "name" : "jrubytruffle",
  "defaultLicense" : "EPL",

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
  "libraries": {
    "RUBY_CORE": {
        "path": "lib/jruby.jar",
        "sha1": "NOCHECK",
        "optional" :"true",
        "license" : "EPL"
    },
    "RUBY_TRUFFLE": {
        "path": "lib/jruby-truffle.jar",
        "sha1": "NOCHECK",
        "optional" :"true",
        "license" : "EPL"
    },
  },
  "projects" : {

    # ------------- Projects -------------

    "jruby-truffle" : {
      "subDir" : "truffle/target/classes",
      "class" : "MavenProject",
      "sourceDirs" : ["src/main/java"],
      "dependencies": [
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_DEBUG",
      ],
    },
    "jruby-lib" : {
      "subDir" : "lib",
      "class" : "MavenProject",
      "sourceDirs" : ["src/main/java"],
      "dependencies": [],
    },

  },
  "licenses" : {
    "EPL" : {
      "name" : "EPL",
      "url" : "https://opensource.org/licenses/EPL-1.0",
    },
  },
  "distributions" : {

    # ------------- Distributions -------------

    "RUBY": {
        "mainClass": "org.jruby.Main",
        "dependencies": ["jruby-truffle", "RUBY_CORE", "RUBY_TRUFFLE"],
        "exclude" : [
          "truffle:JLINE",
        ],
        "distDependencies" : [
          "truffle:TRUFFLE_API",
          "truffle:TRUFFLE_DEBUG",
        ],
        "description" : "JRuby+Truffle",
        "license": "EPL"
    },
    "RUBY-ZIP": {
        "dependencies": [
            "jruby-lib",
        ],
        "exclude" : [
          "truffle:JLINE",
        ],
        "distDependencies" : [],
        "description" : "JRuby+Truffle Native Libs",
        "license": "EPL"
    },

  },
}
