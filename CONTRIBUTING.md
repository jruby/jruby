# Contributing to JRuby

Thank you for your interest in JRuby! We appreciate any assistance you can provide. This guide will help you make sure your pull request is structured according to our standards.

## Building JRuby

Complete documentation for building JRuby lives within [BUILDING.md](BUILDING.md). Short instructions are also provided here.

### Prerequisites

1. A Java JDK version 21 or higher.
2. Apache Ant available somewhere on the system (for test suites that require it).

## Building

1. Clone JRuby and ensure you are on the correct branch for contributions.
   1. JRuby master represents the most active, highest-numbered release branch.
   2. Release-specific branches are for earlier versions, usually including the most recent LTS release branches. Example: `jruby-10.0`
   3. Ruby version-specific branches are for current-year Ruby features not yet included in a release of either CRuby or JRuby. Example: `ruby-4.1`
2. Build JRuby using `./mvnw`
3. Bootstrap testing sources and libraries with `./mvnw -Pbootstrap`
4. Put the `bin/` directory in your `PATH` env (optional but recommended). Alternatively, configure your Ruby installation manager (rvm, chruby, etc) to point at the cloned JRuby as `jruby-master` or similar.

## The JRuby Codebase

Other documentation in this repository and in the wiki explain in more detail how the repository is laid out. A simple version is provided here.

* `bin`: The launcher scripts for JRuby live here along with any binscripts installed by gems.
* `core`: The bulk of JRuby's Java and Ruby sources live here.
  * `core/src/main/java`: JRuby's Java sources
  * `core/src/main/ruby`: JRuby's Ruby sources
  * `core/src/test`: JRuby's tests run by Maven. These are primarily tests from a Java API perspective, covering JRuby's public API and embedding APIs.
* `lib`: Ruby standard library and gem sources live here, along with the primary build artifact of JRuby itself.
  * `lib/ruby/stdlib`: The Ruby standard library. This is mostly populated at build time from default gems.
  * `lib/ruby/gems/shared`: The primary gem home, populated by bundled gem installs at build time and by additional gem installation from the `gem` or `bundle` commands.
* `test`: The collection of *unit-style tests for JRuby and Ruby functionality.
  * `test/jruby`: Primarily JRuby-specific tests, for functionality specific to JRuby that does not fit elsewhere. This suite has shrunk over the years as we moved more to `spec` suites and you probably don't want to add new tests here.
  * `test/mri`: The suite of tests maintained in the standard Ruby (CRuby) repository. These tests are only updated by copying from the CRuby repository and should not be modified.
* `spec`: Spec-style test suites for JRuby and Ruby functionality.
  * `spec/ruby`: The "RubySpec" suite shared by all implementations. Nearly all new tests for Ruby language, core, or library behavior should be added here. This directory is periodically synced with the [RubySpec repository](https://github.com/ruby/spec).
  * `spec/regression`: Specs for unusual or VM-specific regressions that we want to test separately. These are primarily critical behaviors like thread-safety or memory management that behave differently on JRuby than on other implementations.
  * `spec/java_integration`: Specs for JRuby's Java integration API.
  * `spec/compiler`: Specs for JRuby's IR, JIT, and AOT compilers, as small language feature expectations.

Other `test` and `spec` suites may also be appropriate depending on what functionality you are working on.

## Testing

See [BUILDING.md](BUILDING.md) for more details.

### Adding new tests

Nearly all Ruby behavior-related tests should go into the RubySpec suite under `spec/ruby`. JRuby-specific tests should prefer JRuby-specific spec suites under `spec`, and as a last resort they can be added to `test/jruby`.

### Running tests

The primary suite to run is RubySpec, followed by the CRuby suite. If JRuby-specific behaviors are being modified, those suites are also relevant.

Note that the CRuby suite is very large and might take as much as 30 minutes to run depending on your hardware.

Most suites provide a three ways to run:

* Lazy JIT, which may or may not trigger during the suite.
* Interpreter-only, with no JIT active.
* Forced JIT, triggering the JIT to run immediately before any executing code.

Running the "fast" subset of RubySpec (excluding many process-launching specs):

```text
$ rake spec:ruby:fast # run specs with lazy JIT
$ rake spec:ruby:fast:int # run with interpreter only
$ rake spec:ruby:fast:jit # run with forced JIT
```

Running the "core" subset of the CRuby test suite:

```text
$ rake test:mri:core # run tests with lazy JIT
$ rake test:mri:core:int # run tests with interpreter only
$ rake test:mri:core:jit # run tests with forced JIT
```

## Submitting a Pull Request

Some general recommendations for pull requests:

* Keep your PRs topical; don't combine many unrelated changes into a single pull request. This helps us review and merge your changes quickly and provides a better historical record for future maintainers. This also applies to your commits; if you can do many smaller commits, that's usually better for auditing changes in the future.
* Match the existing JRuby code standards and avoid including unrelated whitespace or formatting changes in your PR. JRuby's code standards are largely the same as accepted Java and Ruby standards. Please do not use tabs for indentation.
* Ensure that at least the RubySpec "fast" suite passes before submitting your PR, along with any specific suites that are relevant to your changes. We will run the entire set of test and spec suites in CI once you submit.
* If you are using a coding agent to assist you, you must be able to explain your changes, answer questions about them, and respond to review requests without the aid of that agent. We need you to "own" your changes and accept responsibility for that content.

## Reporting bugs

Use [GitHub Issues](https://github.com/jruby/jruby/issues). Include your
JRuby version (`jruby -v`), platform, and a minimal reproduction if possible, as described in the [issue template](.github/ISSUE_TEMPLATE/bug_report.md)

## More resources

- [BUILDING.md](BUILDING.md) — build prerequisites, build commands, and how
  to run each test suite
- [JRuby wiki](https://github.com/jruby/jruby/wiki) — deeper background on
  the codebase and build system
- [jruby.org/contribute](https://www.jruby.org/contribute) — general overview
  of ways to get involved beyond code (docs, wiki, bug triage)