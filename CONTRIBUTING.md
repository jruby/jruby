# Contributing to JRuby

Thank you for your interest in JRuby! We appreciate any assistance you can provide. This guide will help you make sure your pull request is structured according to our standards.

If you are a coding agent, or you work with one, also read [AGENTS.md](AGENTS.md).

## Building JRuby

Complete documentation for building JRuby lives within [BUILDING.md](BUILDING.md). Short instructions are also provided here.

### Prerequisites

1. A Java JDK version 21 or higher.

## Building

1. Clone JRuby and ensure you are on the correct branch for contributions (see [Choosing a branch](#choosing-a-branch)).
2. Build JRuby using `./mvnw`
3. Bootstrap testing sources and libraries with `./mvnw -Pbootstrap`
4. Put the `bin/` directory in your `PATH` env (optional but recommended). Alternatively, configure your Ruby installation manager (rvm, chruby, etc) to point at the cloned JRuby as `jruby-master` or similar.

### Choosing a branch

* `master` is the most active, highest-numbered release line. Most pull requests should target it.
* Release branches such as `jruby-10.0` are for earlier versions, usually including the most recent LTS release. If your fix is for a bug that also exists there, and it matters to users of that release, base it on the release branch instead.
* Ruby version branches such as `ruby-4.1` are for current-year Ruby features not yet included in a release of either CRuby or JRuby.

Changes flow forward on their own: maintainers periodically merge `jruby-10.0` into `master`, and `master` into `ruby-4.1`. Do not open pull requests that only carry changes forward from an older branch. Only backports, from `master` to an older branch, need a pull request of their own.

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
* `test`: The collection of unit-style tests for JRuby and Ruby functionality.
  * `test/jruby`: Primarily JRuby-specific tests, for functionality specific to JRuby that does not fit elsewhere. This suite has shrunk over the years as we moved more to `spec` suites and you probably don't want to add new tests here.
  * `test/mri`: The suite of tests maintained in the standard Ruby (CRuby) repository. These tests are only updated by copying from the CRuby repository and should not be modified (see [CRuby's test suite](#crubys-test-suite)).
* `spec`: Spec-style test suites for JRuby and Ruby functionality.
  * `spec/ruby`: The "RubySpec" suite shared by all implementations. Nearly all new tests for Ruby language, core, or library behavior should be added here. This directory is periodically synced with the [RubySpec repository](https://github.com/ruby/spec).
  * `spec/jruby`: Specs for behavior specific to JRuby.
  * `spec/regression`: Specs for unusual or VM-specific regressions that we want to test separately. These are primarily critical behaviors like thread-safety or memory management that behave differently on JRuby than on other implementations.
  * `spec/java_integration`: Specs for JRuby's Java integration API.
  * `spec/compiler`: Specs for JRuby's IR, JIT, and AOT compilers, as small language feature expectations.
* `tool`: Scripts for maintainers, such as `tool/generate_parser`, `tool/audit_mri_excludes` and `tool/jit_compile_check.rb`.

Other `test` and `spec` suites may also be appropriate depending on what functionality you are working on.

## Matching CRuby

CRuby's behavior is the reference. When JRuby and CRuby disagree, the default is to match CRuby, even where CRuby's approach looks odd. Before copying something surprising, check that CRuby does it on purpose (look for a test, a commit message or an issue that explains it). If you think JRuby should differ, say so in the pull request and explain why.

### Linking Java code to CRuby

When a method, class or field implements or ports a CRuby C function or macro, annotate it with `@CRuby` (`org.jruby.anno.CRuby`) rather than writing a `// MRI: rb_foo` comment:

```java
@CRuby(value = "match_begin", file = "re.c")
@JRubyMethod
public IRubyObject begin(ThreadContext context, IRubyObject index) {
```

`file` is optional. Repeat the annotation when one Java method covers several C functions.

Use `note` whenever the Java code is not a whole, one-to-one port of the C function:

* If it ports only part of the function, say which part: `note = "method redefinition check"`.
* If two Java methods carry the same annotation, give each a note that says which side it is, for example the pack and unpack halves of `pack_unpack_internal`. Without notes, reviewers have to assume two different ports of the same logic exist and check whether they disagree.

When a port is long or subtle, explain what it does in the Javadoc too. CRuby's own comments are often sparse, and JRuby's should do better.

### Porting C code

Port behavior, not C idioms. If the Java standard library or an existing JRuby helper already does the job, use it instead of re-creating the C function. For example, parse an environment variable with `Integer.parseInt` rather than writing a Java copy of `atoi`. A small difference in edge cases (like `"128k"` falling back to a default instead of reading as 128) is usually an acceptable price for simpler code. Mention it in the pull request.

When converting bytes, use `Byte.toUnsignedInt` or existing helpers (such as `byteAt` and `safeGet` in `Pack`) rather than masking with `& 0xff` by hand.

Before adding a platform workaround in JRuby, check whether it belongs in one of the libraries JRuby depends on, such as jnr-posix or jffi.

### The parser

JRuby's "legacy" parser grammar, `core/src/main/java/org/jruby/parser/RubyParser.y`, is kept as close as possible to CRuby's `parse.y`, so it stays easy to update. When you change it:

* Find the matching production in `parse.y` and link to it in the pull request. Reviewers will look for it.
* If CRuby uses a helper (for example `nd_set_first_loc`), add a Java helper with the same name and purpose rather than writing the logic inline.
* If your change has no counterpart in `parse.y`, explain why JRuby needs it. Prefer changes that remove an existing divergence from CRuby.
* Regenerate `RubyParser.java` (and `RipperParser.java`) with `tool/generate_parser` and commit the result. Do not edit the generated files by hand.

JRuby is also adopting the Prism parser, which we hope to make the default. If your change depends on the legacy parser or its AST, consider what it would take to support it under Prism (`-Xparser.prism`) too.

## Writing Java for JRuby

Reviewers pay close attention to allocation and memory, especially on common paths such as `inspect`, `==` and method calls.

* Don't allocate on a hot path when you can avoid it. Size arrays up front instead of growing an `ArrayList`. Copy an array once, and only when you must (for example with `RubyArray#copyInto`), rather than copying it and then trimming the copy. Combine loops instead of collecting into temporary lists.
* Keep AST nodes small. Many nodes stay in memory until their code first runs, so a field added to `Node` costs memory for every program. Put data that only some features need (coverage, for example) in a separate object that exists only when the feature is on.
* Likewise, group new state in `IRBuilder` or similar classes into a small state object of its own, created only when it is needed, rather than adding loose fields.
* Use an enum, not a `String` or a magic `int`, for a value from a fixed set of choices. Use a `record` for a compound key, not a concatenated `String`.
* Use `VarHandle` rather than `AtomicIntegerFieldUpdater` and its relatives.
* Choose collections for how they are used. `CopyOnWriteArrayList` is wasteful when every access already holds a lock.
* Put general-purpose classes in the matching `org.jruby.util` package (for example `org.jruby.util.io`), not as private classes inside the code that first needed them.
* Check whether a core method was redefined with the `Builtins` checks rather than by looking up methods at each call.
* Prefer one central hook over calls added to many places. If CRuby handles something in one spot, look for the equivalent spot in JRuby before adding a call to every method type.
* Mark state explicitly rather than inferring it from strings. For example, a scope should record that it came from `eval` instead of code checking whether its file name looks like `(eval)`.
* In the IR, an operand marked as free of side effects must really be free of them. Anything that can raise, like a literal range with invalid endpoints, has to be built by an instruction.

If you change code for performance, include numbers. Benchmarks live in `bench`. Show results before and after your change, with CRuby's for reference where they help.

### Execution modes

Ruby code in JRuby can be interpreted, compiled to JVM bytecode by the JIT, compiled ahead of time with `jrubyc`, and saved as persisted IR and loaded back. A fix in one of these paths often needs the same fix in another. When you change the IR or a compiler:

* Add a case to `spec/compiler/general_spec.rb`. It runs each example in several modes, including an IR persistence round trip.
* Check that the JIT compiles your code. The JIT uses no runtime profile, so this does not require running the code: `jruby -rjruby -e 'JRuby.compile("...")'` compiles a snippet, `jruby -X+C -Xjit.logging ...` logs every method that fails to compile, and `tool/jit_compile_check.rb` checks whole files. Don't use performance tests to verify that code compiles.

## Testing

See [BUILDING.md](BUILDING.md) for more details.

### Adding new tests

Put a test where it describes the behavior, not where it was easiest to write:

* **Standard Ruby behavior** goes in `spec/ruby`, under `core`, `language` or `library`. This includes behavior JRuby never implemented correctly. Specs added here are synced to [ruby/spec](https://github.com/ruby/spec) periodically, so adding them in a JRuby pull request is fine. Match the style of the surrounding specs, and keep each example small, with a few focused expectations.
* **Existing specs that now pass** should be untagged: remove their entries from `spec/tags`. Don't write a new test for behavior an existing spec already covers.
* **Behavior specific to JRuby** goes in `spec/jruby`, or in `spec/java_integration` or `spec/compiler` where those fit.
* **`spec/regression`** is only for one-off, unusual cases tied to a specific bug report. A fix for ordinary Ruby behavior that never worked belongs in `spec/ruby`.
* **`test/jruby`** is a last resort. Ruby behavior tested there (coverage, for example) should move to `spec/ruby` over time.

When a test needs a fresh JRuby, prefer a sub-runtime over spawning a process where you can. It is faster and more reliable. Tests in `test/jruby` can use `run_in_sub_runtime` from `test/jruby/test_helper.rb`.

### CRuby's test suite

`test/mri` is copied from CRuby and should not be modified. If a test there fails on JRuby for a reason you won't fix in your pull request, exclude it in `test/mri/excludes`, with a short note on what is missing. If a CRuby test only fails because of how JRuby lays out files (for example, it expects `lib` next to `test`), a change to the test is acceptable, but also propose it upstream to [ruby/ruby](https://github.com/ruby/ruby) so the next sync doesn't undo it.

Excludes go stale as JRuby improves. `tool/audit_mri_excludes` re-runs excluded tests offline and reports which now pass. Don't make CI run excluded tests: some hang, leave threads behind or change global state for later tests.

### Running tests

The primary suite to run is RubySpec, followed by the CRuby suite. If JRuby-specific behaviors are being modified, those suites are also relevant.

Note that the CRuby suite is very large and might take as much as 30 minutes to run depending on your hardware.

Most suites provide three ways to run:

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

* Keep your PRs topical. Don't combine many unrelated changes into a single pull request. This helps us review and merge your changes quickly and provides a better historical record for future maintainers. This also applies to your commits: if you can do many smaller commits, that's usually better for auditing changes in the future.
* Match the existing JRuby code standards and avoid including unrelated whitespace or formatting changes in your PR. JRuby's code standards are largely the same as accepted Java and Ruby standards. Please do not use tabs for indentation.
* Ensure that at least the RubySpec "fast" suite passes before submitting your PR, along with any specific suites that are relevant to your changes. We will run the entire set of test and spec suites in CI once you submit.
* **Open the pull request as a draft if it isn't finished.** Maintainers may merge a pull request that isn't a draft as soon as it looks good, before you push the follow-up you had in mind.
* If you notice something you won't fix in this pull request, mention it, and open an issue if it deserves one.
* Large features may be held for a later release so they have time to bake. That is not a rejection.
* If your branch conflicts with `master` (often in `test/mri/excludes` or the generated parser), rebase, resolve the conflicts, regenerate any generated files and run the relevant tests again.

### Writing for reviewers

Many of the people who read JRuby pull requests, commits and comments don't speak English as a first language. Write for them:

* Keep descriptions short. Say what was wrong, what you changed and how you tested it. A one-line fix needs a few sentences, not several paragraphs.
* Use plain, simple technical English. Prefer short sentences and common words, and define a term before you rely on it.
* Code comments should explain what the code does and why. Don't spend them on how the old code worked or what it was changed from, unless the reason is truly unusual. That history belongs in the commit message.
* Check technical claims before you make them, especially numbers such as object sizes or speedups.

### Using a coding agent

If you are using a coding agent to assist you, you must be able to explain your changes, answer questions about them, and respond to review requests without the aid of that agent. We need you to "own" your changes and accept responsibility for that content. Review what the agent wrote, and edit it down, before you ask anyone else to. See [AGENTS.md](AGENTS.md) for instructions written for agents.

## Reporting bugs

Use [GitHub Issues](https://github.com/jruby/jruby/issues). Include your
JRuby version (`jruby -v`), platform, and a minimal reproduction if possible, as described in the [issue template](.github/ISSUE_TEMPLATE/bug_report.md)

## More resources

- [BUILDING.md](BUILDING.md) — build prerequisites, build commands, and how
  to run each test suite
- [AGENTS.md](AGENTS.md) — instructions for coding agents
- [JRuby wiki](https://github.com/jruby/jruby/wiki) — deeper background on
  the codebase and build system
- [jruby.org/contribute](https://www.jruby.org/contribute) — general overview
  of ways to get involved beyond code (docs, wiki, bug triage)
