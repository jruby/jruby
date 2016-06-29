This is the library that implements the Ruby C API for JRuby+Truffle. We
include the built version in the repository (under the lib directory) because
building it requires some complex dependencies.

To rebuild it, you need Sulong checked out and built. Then set `SULONG_DIR`
and run:

```
$ bin/jruby bin/jruby-cext-c truffle/src/main/c/cext
```
