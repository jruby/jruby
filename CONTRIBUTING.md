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
* Release branches such as `jruby-10.0` are for earlier versions, usually including the most recent LTS release. If you fix a bug that also exists there, and users of that release can see it (for example, JRuby behaves differently from CRuby), base the fix on the release branch instead. Maintainers often ask for this.
* Ruby version branches such as `ruby-4.1` are for current-year Ruby features not yet included in a release of either CRuby or JRuby.

Changes flow forward on their own: maintainers periodically merge `jruby-10.0` into `master`, and `master` into `ruby-4.1`. Do not open pull requests that only carry changes forward from an older branch. Only backports, from `master` to an older branch, need a pull request of their own.

Large or risky changes go to `master` even when they fix a bug in a release branch, so they can bake in `jruby-head` builds first. A backport can follow later.

If you need to move a pull request to a different branch, rebase it locally, force-push, and change the base branch of the same pull request (with the same Edit button as the title). Don't open a new pull request.

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
* `bench`: Benchmarks.
* `rakelib`: The Rake tasks that run the test suites.
* `tool`: Scripts for maintainers, such as `tool/generate_parser`, `tool/audit_mri_excludes` and `tool/jit_compile_check.rb`.

Other `test` and `spec` suites may also be appropriate depending on what functionality you are working on.

JRuby is mostly Java, built with Maven (`./mvnw`), with Ruby test suites run through Rake and mspec. There is no Makefile, and the repository does not use npm, Python, RuboCop or `bundle exec rspec`.

## Matching CRuby

CRuby's behavior is the reference. When JRuby and CRuby disagree, the default is to match CRuby, even where CRuby's approach looks odd. That includes the exact text of error messages, because real code depends on it. Before copying something surprising, check that CRuby does it on purpose (look for a test, a commit message or an issue that explains it). If CRuby's behavior and its documentation disagree, match the behavior and raise the question at [bugs.ruby-lang.org](https://bugs.ruby-lang.org). If you think JRuby should differ, say so in the pull request and explain why.

For a new Ruby feature, check your change and specs against a build of CRuby's development branch, not only the latest release.

### Fix the cause, not the symptom

Find out why something fails before you change anything. Reviewers push back on changes that make a failure go away without explaining it, such as:

* adding `.gitignore` entries for files that something in the build should not have created,
* retrying, waiting longer or rescuing an error to hide a race,
* changing a test from CRuby so it passes, without knowing whether the test or JRuby is wrong.

If you can't find the cause, say so in the pull request, and describe what you know.

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

Explain what the code does in its Javadoc, not next to the annotation. When a port is long or subtle, explain it well. CRuby's own comments are often sparse, and JRuby's should do better.

### Porting C code

Port behavior, not C idioms. If the Java standard library or an existing JRuby helper already does the job, use it instead of re-creating the C function. For example, parse an environment variable with `Integer.parseInt` rather than writing a Java copy of `atoi`. A small difference in edge cases (like `"128k"` falling back to a default instead of reading as 128) is usually an acceptable price for simpler code. Mention it in the pull request.

A straight port of a long C function makes one huge Java method. Split it into smaller methods along its logical steps. The JVM optimizes many small methods much better than one big one, and they are easier to review.

When converting bytes, use `Byte.toUnsignedInt` or existing helpers (such as `byteAt` and `safeGet` in `Pack`) rather than masking with `& 0xff` by hand. A bare mask is easy to miss or delete in a later change.

Native and platform-specific logic usually belongs in the libraries JRuby builds on, such as jnr-posix, jnr-ffi, jffi, jnr-enxio and jnr-unixsocket, where other projects can use it too. Check there before adding a workaround to JRuby, and mention in the pull request if you think code should move there later.

### The parser

JRuby's "legacy" parser grammar, `core/src/main/java/org/jruby/parser/RubyParser.y`, is kept as close as possible to CRuby's `parse.y`, so it stays easy to update. When you change it:

* Find the matching production in `parse.y` and link to it in the pull request. Reviewers will look for it.
* Reuse the productions CRuby uses (for example `call_op2`) instead of adding new ones that cover the same case.
* If CRuby uses a helper (for example `nd_set_first_loc`), add a Java helper with the same name and purpose rather than writing the logic inline.
* If your change has no counterpart in `parse.y`, explain why JRuby needs it. Prefer changes that remove an existing divergence from CRuby.
* Regenerate `RubyParser.java` (and `RipperParser.java`) with `tool/generate_parser` and commit the result. Do not edit the generated files by hand. If the generated files conflict with `master`, rebase and regenerate them rather than resolving the conflict by hand.

JRuby is also adopting the Prism parser, which we hope to make the default. If your change depends on the legacy parser or its AST, consider what it would take to support it under Prism (`-Xparser.prism`) too.

## Writing Java for JRuby

Reviewers pay close attention to allocation and memory, especially on common paths such as `inspect`, `==`, method calls and IO.

### Allocation and memory

* Don't allocate on a hot path when you can avoid it. Size arrays up front instead of growing an `ArrayList`. Copy an array once, and only when you must (for example with `RubyArray#copyInto`), rather than copying it and then trimming the copy. Combine loops instead of collecting into temporary lists.
* Pay only for features in use. Code that supports the fiber scheduler, coverage or another optional feature should allocate nothing when that feature is off. Check whether the feature is active first, and set up its state lazily.
* Reuse objects that are only ever used by one thread at a time. For example, a waiter that belongs to one thread or fiber can be created once and kept on the `RubyThread`, instead of being allocated on every wait. Don't allocate a new wrapper (like a `WeakReference`) every time a value changes when you can store a plain value instead.
* Keep AST nodes small. Many nodes stay in memory until their code first runs, so a field added to `Node` costs memory for every program. Put data that only some features need (coverage, for example) in a separate object that exists only when the feature is on.
* Put new state on the most specific class that needs it. State only sockets use belongs on `RubyBasicSocket`, not on every IO object. Likewise, group new state in `IRBuilder` or similar classes into a small state object of its own, created only when it is needed.
* Choose collections for how they are used. `ConcurrentHashMap` is heavy for small maps, and `CopyOnWriteArrayList` is wasteful when every access already holds a lock.

### Code

* Use an enum, not a `String` or a magic `int`, for a value from a fixed set of choices. Use a `record` for a compound key, not a concatenated `String`.
* Use `VarHandle` rather than `AtomicIntegerFieldUpdater` and its relatives.
* Call Ruby methods from Java through a cached `CallSite`, not an uncached `callMethod`, on any path that runs often.
* Check whether a core method was redefined with the `Builtins` checks rather than by looking up methods at each call.
* Prefer one central hook over calls added to many places. If CRuby handles something in one spot, look for the equivalent spot in JRuby before adding a call to every method type.
* Before writing a helper, search for an existing one that does the same thing (like the check for whether a stream is a terminal), and use or extend it instead of adding a second copy.
* Put general-purpose classes in the matching `org.jruby.util` package (for example `org.jruby.util.io`), not as private classes inside the code that first needed them.
* Mark state explicitly rather than inferring it from strings. For example, a scope should record that it came from `eval` instead of code checking whether its file name looks like `(eval)`.
* In the IR, an operand marked as free of side effects must really be free of them. Anything that can raise, like a literal range with invalid endpoints, has to be built by an instruction.
* Keep lines short, including one-line `if` statements. If a one-line `if` runs past about 100 characters, give it a block.

### APIs and deprecations

* New methods that create Ruby objects belong in `org.jruby.api.Create`. Keep explicit `begin` offsets in byte-array signatures, even when the value is `0`, because code that assumes a zero offset has caused bugs.
* When you add an overload, match the parameters of the overloads next to it. Take a `ThreadContext` when the code needs one. If the code never uses the context at all, taking `Ruby` is fine.
* Cleaning up calls to deprecated methods is welcome, but each change must be zero-sum: the replacement must behave the same, and `context` must already be in scope (don't call `runtime.getCurrentContext()` just to reach the new method). If there is no good replacement, say so instead of writing something more awkward. Group these changes into logical batches, not one pull request per call, and keep the diffs small, because they cause merge conflicts when `jruby-10.0` is merged forward.

### Ruby code in the build

Rake tasks in `rakelib` and other Ruby tooling should be idiomatic Ruby. Don't add mutable constants at the top level. Put helpers in a namespace under `JRuby::Rake`, as `JRuby::Rake::JavaCommand` in `rakelib/commands.rake` does. Prefer `&.` and `Array(...)` to longer nil checks and conversions.

### Comments

Comments should explain what the code does and why, briefly. Don't add comment blocks that restate what the code already says, and don't spend comments on how the old code worked or what it was changed from, unless the reason is truly unusual. That history belongs in the commit message.

### Performance changes

If you change code for performance, show the evidence in the pull request:

* Include the benchmark itself, not only its results, so reviewers can run it. Ruby benchmarks go in `bench`. For Java-level changes, a [JMH](https://github.com/openjdk/jmh) benchmark avoids Ruby overhead. Don't put benchmarks under `src`, because they would ship with JRuby.
* Show numbers from before and after your change, measured the same way. Results without a baseline can't be compared. CRuby's numbers help too.
* Make sure the results support every claim in the description. If you say three kinds of input got faster, show all three. Report slowdowns with their numbers, not only in passing.
* When the change is about allocation, measure allocation too.

### Execution modes

Ruby code in JRuby can be interpreted, compiled to JVM bytecode by the JIT, compiled ahead of time with `jrubyc`, and saved as persisted IR and loaded back. A fix in one of these paths often needs the same fix in another. When you change the IR or a compiler:

* Add a case to `spec/compiler/general_spec.rb`. It runs each example in several modes, including an IR persistence round trip.
* Check that the JIT compiles your code. The JIT uses no runtime profile, so this does not require running the code: `jruby -rjruby -e 'JRuby.compile("...")'` compiles a snippet, `jruby -X+C -Xjit.logging ...` logs every method that fails to compile, and `tool/jit_compile_check.rb` checks whole files. Don't use performance tests to verify that code compiles.
* To run specs with the JIT compiling everything before it runs, set the JIT threshold to zero: `bin/jruby spec/mspec/bin/mspec ci -T-Xjit.threshold=0 <spec>`, or `rake spec:ruby:fast:jit`. CI already runs the suites this way.

## Testing

See [BUILDING.md](BUILDING.md) for more details.

### Adding new tests

Put a test where it describes the behavior, not where it was easiest to write:

* **Standard Ruby behavior** goes in `spec/ruby`, under `core`, `language` or `library`. This includes behavior JRuby never implemented correctly. Specs about how Ruby code is parsed, interpreted or compiled belong in `language`, even when they use a core class.
* **Existing specs that now pass** should be untagged: remove their entries from `spec/tags`. Don't write a new test for behavior an existing spec already covers.
* **Behavior specific to JRuby** goes in `spec/jruby`, or in `spec/java_integration` or `spec/compiler` where those fit.
* **`spec/regression`** is only for one-off, unusual cases tied to a specific bug report. A fix for ordinary Ruby behavior that never worked belongs in `spec/ruby`.
* **`test/jruby`** is a last resort, for behavior that can only be observed through JRuby internals. Ruby behavior tested there (coverage, for example) should move to `spec/ruby` over time.

### Writing specs for `spec/ruby`

`spec/ruby` is synced with [ruby/spec](https://github.com/ruby/spec) in both directions, so specs you add here become part of the shared suite. Adding them in a JRuby pull request is fine.

* Every spec must also pass on CRuby. CI runs `spec/ruby` on several CRuby versions, so guard specs for new behavior with `ruby_version_is`, as described in `spec/ruby/CONTRIBUTING.md`.
* Match the style of the surrounding specs, and keep each example small, with a few focused expectations.
* Name arguments in descriptions the way CRuby's documentation names them, so the spec output says what is being tested.
* Uncommon usage, especially error cases, is still worth a spec.
* Test behavior through Ruby's public API, not by asserting on JRuby's internal state.
* Cover more than one kind of input where it matters (for example, both Integer and Float keys, not only one).
* Code that a spec runs in a separate process belongs in a fixture file, as other specs do it, not in a long inline string.
* Give fixture files names that show what they are. A Ruby file named like a native library (`.so`, `.bundle`) looks like a stray binary, and someone will delete it.

When a test needs a fresh JRuby, prefer a sub-runtime over spawning a process where you can. It is faster and more reliable. Tests in `test/jruby` can use `run_in_sub_runtime` from `test/jruby/test_helper.rb`.

### CRuby's test suite

`test/mri` is copied from CRuby and should not be modified.

* If a test there fails on JRuby for a reason you won't fix in your pull request, exclude it in `test/mri/excludes`, with a short note on what is missing.
* If a test is out of date, don't update it piece by piece in your pull request. The suite is synced from CRuby as a whole, so ask for (or do, in a separate pull request) a sync of the whole file or suite.
* If you believe a CRuby test itself is wrong, first make sure JRuby isn't at fault. Then propose the fix upstream, to [ruby/ruby](https://github.com/ruby/ruby) or to the gem the test comes from (for example ruby/open-uri). A local change that isn't upstream will be overwritten by the next sync.

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

* **Keep each pull request to one concern.** Don't combine unrelated changes, and don't mix refactoring with a fix or an optimization. Moving code around makes the real change hard to find. If a fix needs a refactor, send the refactor first as its own pull request that changes no behavior. Cleanups you notice along the way can go in a later pull request. This also applies to your commits: if you can do many smaller commits, that's usually better for auditing changes in the future.
* Match the existing JRuby code standards and avoid including unrelated whitespace or formatting changes in your PR. JRuby's code standards are largely the same as accepted Java and Ruby standards. Please do not use tabs for indentation.
* **Check your diff for stray files before you push.** Build output, copied directories, editor and IDE files, and generated files you didn't mean to change should not be in the pull request. If a tool keeps creating files that don't belong, find out why instead of adding them to `.gitignore`.
* Ensure that at least the RubySpec "fast" suite passes before submitting your PR, along with any specific suites that are relevant to your changes. We will run the entire set of test and spec suites in CI once you submit.
* **Open the pull request as a draft if it isn't finished,** and mark it ready for review when it is. Maintainers usually don't review drafts unless you ask, and they may merge a pull request that isn't a draft as soon as it looks good, before you push the follow-up you had in mind.
* Keep the history readable. When review leads you to change or drop something, amend or drop the commit that introduced it (`git rebase -i <base branch>`) instead of stacking commits that add and then remove code.
* If you notice something you won't fix in this pull request, mention it, and open an issue if it deserves one.
* Large features may be held for a later release so they have time to bake. That is not a rejection.
* If your branch conflicts with `master` (often in `test/mri/excludes` or the generated parser), rebase, resolve the conflicts, regenerate any generated files and run the relevant tests again.
* You don't need to write release notes. They are generated from the pull requests and issues in each release's milestone.

### Writing for reviewers

Many of the people who read JRuby pull requests, commits and comments don't speak English as a first language. Write for them:

* Keep descriptions short. Say what was wrong, what you changed and how you tested it. A one-line fix needs a few sentences, not several paragraphs.
* Use plain, simple technical English. Prefer short sentences and common words, and define a term before you rely on it.
* Link to the CRuby code you matched or ported, at a specific commit.
* Check technical claims before you make them, especially numbers such as object sizes or speedups. When you explain why something happens, look for the simplest explanation that fits the facts, and test it. A confident but wrong explanation costs reviewers more time than no explanation.

### Checklist before review

Reviewers flag these problems most often. Check each one before you mark a pull request ready, and reviewers can link here instead of explaining them again.

1. **The test is in the wrong place.** Standard Ruby behavior belongs in `spec/ruby`, not `test/jruby` or `spec/regression`. See [Adding new tests](#adding-new-tests).
2. **The explanation is wrong.** Check every claim, such as object sizes, the cause of a speedup, which JIT mode CI uses or whether a CI step is still needed. Prefer the simplest explanation that fits, and say so when you didn't verify something.
3. **The description is too long.** See [Writing for reviewers](#writing-for-reviewers).
4. **A CRuby port has no `@CRuby` annotation, or a partial port has no `note`.** See [Linking Java code to CRuby](#linking-java-code-to-cruby).
5. **Bytes are masked with `& 0xff`.** Use `Byte.toUnsignedInt` or an existing helper.
6. **The change allocates more on a common path,** or allocates for a feature (like the fiber scheduler) even when it is off. See [Allocation and memory](#allocation-and-memory).
7. **Unrelated changes are mixed in.** Refactoring, cleanups and drive-by fixes go in separate pull requests.
8. **Stray files are committed.** Check `git status` and the full diff before every commit.
9. **The symptom is patched instead of the cause.** See [Fix the cause, not the symptom](#fix-the-cause-not-the-symptom).
10. **C code is copied instead of using Java,** like a hand-written `atoi`, or a straight port left as one giant method. See [Porting C code](#porting-c-code).

### Using a coding agent

If you are using a coding agent to assist you, you must be able to explain your changes, answer questions about them, and respond to review requests without the aid of that agent. We need you to "own" your changes and accept responsibility for that content. Review what the agent wrote, check its claims, and edit it down before you ask anyone else to read it. See [AGENTS.md](AGENTS.md) for instructions written for agents.

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
