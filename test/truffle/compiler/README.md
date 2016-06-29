Any `*.sh` file placed in this directory will be run as part of the
`jt test compiler` tests. Tests report failure by returning a non-zero exit
code. The tests are run with an environment variable `JAVACMD` set to something
that has Graal (unless `--no-java-cmd` is used.)

Note that tests in this directory should run `ruby` as a command, rather than
`bin/jruby`. `jt` will put `bin` on the path for you. This is so that
people packaging JRuby+Truffle in some unusual way can run their external
packed version against these tests, and the tests aren't hardcoded to always
run `bin/jruby`.
