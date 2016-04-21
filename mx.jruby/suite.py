suite = {
  "mxversion" : "5.6.6",
  "name" : "jrubytruffle",
  "defaultLicense" : "GPLv2", # FIXME

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
    "RUBY_COMPLETE": {
        "path": "maven/jruby-complete/target/jruby-complete-graal-vm.jar",
        "sha1": "NOCHECK",
        "optional" :"true",
        "license" : "GPLv2"
    },
    "RUBY_TRUFFLE": {
        "path": "lib/jruby-truffle.jar",
        "sha1": "NOCHECK",
        "optional" :"true",
        "license" : "GPLv2"
    },
  },
  "projects" : {

    # ------------- Projects -------------

    "jruby-truffle" : {
      "subDir" : "",
      "class" : "MavenProject",
      "sourceDirs" : ["src/main/java"],
      "dependencies": [
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_DEBUG",
      ],
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

    "RUBY": {
        "mainClass": "org.jruby.Main",
        "dependencies": ["jruby-truffle", "RUBY_COMPLETE", "RUBY_TRUFFLE"],
        "exclude" : [
          "truffle:JLINE",
        ],
        "distDependencies" : [
          "truffle:TRUFFLE_API",
          "truffle:TRUFFLE_DEBUG",
        ],
        "description" : "JRuby+Truffle",
    },

  },
}
