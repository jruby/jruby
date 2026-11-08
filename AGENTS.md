# Instructions for coding agents

This file is for coding agents working in the JRuby repository. Read [CONTRIBUTING.md](CONTRIBUTING.md) first. Everything there applies to you. This file adds the points agents get wrong most often, and practical notes for working in this checkout.

The person you work for is responsible for every change you make. They must be able to explain it and answer review questions without you. Write code and text they can read, check and defend.

## Before you write code

* **Find CRuby's implementation first.** CRuby's behavior is the reference, and reviewers compare your change against CRuby's source. Look up the C function in [ruby/ruby](https://github.com/ruby/ruby), and link to the exact lines (with a commit hash in the URL) in the pull request description.
* **Look for an existing helper** in JRuby or the Java standard library before writing one. Don't port C utilities like `atoi` or `memchr` by hand when Java already does the job.
* **Look for one central place** to make a change, as CRuby often does, before adding calls to many call sites.
* **Choose the right branch.** Most work targets `master`. Never open a pull request that only merges changes forward from `jruby-10.0` to `master`, or from `master` to `ruby-4.1`. Maintainers do that.

## Writing code

* Annotate every port from CRuby with `@CRuby`. Add `note` when the method ports only part of the C function, or when another Java method has the same annotation. Name the part in a few words (`note = "method redefinition check"`). A bare annotation on a partial port confuses reviewers.
* Use enums for fixed sets of values, records for compound keys, `VarHandle` for atomic fields and `Byte.toUnsignedInt` instead of `& 0xff`.
* Avoid new allocations on common paths, and don't add fields to AST nodes. See "Writing Java for JRuby" in CONTRIBUTING.md.
* Write short comments that say what the code does now and why. Don't narrate how the code used to work, what you changed, or what you tried first.
* Don't edit generated files. `RubyParser.java` and `RipperParser.java` come from `RubyParser.y` (see below).

## Writing tests

Agents tend to put tests in `test/jruby` or `spec/regression`, because those files look easy to extend. Usually that is the wrong place:

1. Standard Ruby behavior goes in `spec/ruby` (`core`, `language` or `library`), as small mspec examples that match the style of the file around them. This is true even when the behavior is something JRuby never got right.
2. If an existing spec in `spec/ruby` now passes, remove its tag from `spec/tags` instead of writing a new test.
3. Only behavior specific to JRuby goes in `spec/jruby` (or `spec/java_integration`, `spec/compiler`).
4. `spec/regression` is only for unusual cases tied to one bug report.
5. Don't modify `test/mri`. Exclude failing tests in `test/mri/excludes` with a short reason.

For IR or compiler changes, add a case to `spec/compiler/general_spec.rb`, which runs each example in several execution modes, including an IR persistence round trip.

## Writing pull requests and comments

Many JRuby reviewers read English as a second language, and they read a lot of pull requests. Long, dense text slows them down and has drawn complaints in review.

* State what was wrong, what you changed and how you tested it. Stop there.
* Use plain words and short sentences. Avoid invented terms and jargon you haven't defined.
* Match the length to the change. A two-line fix needs two or three sentences.
* Don't claim things you haven't checked. For example, object references on a modern JVM are usually 4 bytes (compressed pointers), not 8. Measure performance before describing it, and include the numbers.
* List anything you noticed but didn't fix, briefly, so a maintainer can decide whether to open an issue.
* Open the pull request as a draft if any work remains. Maintainers merge non-draft pull requests quickly.

## Building and testing in this checkout

These notes fill gaps in [BUILDING.md](BUILDING.md).

* **Rebuild `lib/jruby.jar` after changing Java code** with one reactor build: `./mvnw -q -o -DskipTests -pl core,shaded package`. Building `core` alone does not update `lib/jruby.jar`, so `bin/jruby` would keep running the old code. Drop `-o` if Maven needs to download something.
* **Bootstrap once before running Ruby tests**: `./mvnw -Pbootstrap -DskipTests`. Without it, the stdlib and gems that the tests need are missing. It is slow, so only run it when they are missing.
* **Run one spec file** with `bin/jruby spec/mspec/bin/mspec-run <path>`, or `bin/jruby spec/mspec/bin/mspec ci <path>` to skip tagged failures.
* **Run one CRuby test file** with `bin/jruby -Itest/tool/lib -Itest/mri/lib -Itest/mri <path> --excludes-dir=test/mri/excludes`. On a slow or busy machine, add `--timeout-scale=10`, because many tests start a new JRuby and time out otherwise.
* **Regenerate the parser** with `PATH=<dir with jay>:$PATH tool/generate_parser` after editing `RubyParser.y`. `jay` is not packaged anywhere, so build it from [jruby/jay](https://github.com/jruby/jay) (`cd src && make`).
* **Check that the JIT compiles your code** with `bin/jruby -rjruby -e 'JRuby.compile("...")'` for a snippet, or `bin/jruby -J-XX:-OmitStackTraceInFastThrow tool/jit_compile_check.rb <files>` for whole files.
* **Audit MRI excludes** with `bin/jruby tool/audit_mri_excludes <TestClass ...>`. It runs excluded tests offline and reports which now pass.
