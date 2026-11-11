# Instructions for coding agents

This file is for coding agents working in the JRuby repository. Read [CONTRIBUTING.md](CONTRIBUTING.md) first. Everything there applies to you. This file adds the points agents get wrong most often, and practical notes for working in this checkout.

The person you work for is responsible for every change you make. They must be able to explain it and answer review questions without you. Write code and text they can read, check and defend.

## The mistakes reviewers flag most often

Go through the [checklist before review](CONTRIBUTING.md#checklist-before-review) in CONTRIBUTING.md before you finish. These problems come up again and again in reviews of agent-assisted pull requests.

## Before you write code

* **Read the real build setup.** JRuby is mostly Java, built with Maven (`./mvnw`), and its Ruby tests run through Rake and mspec. There is no Makefile. The project does not use npm, Python, RuboCop or `bundle exec rspec`. Don't assume a generic project layout, and don't add tooling for one.
* **Find CRuby's implementation first.** CRuby's behavior is the reference, and reviewers compare your change against CRuby's source. Look up the C function in [ruby/ruby](https://github.com/ruby/ruby), and link to the exact lines (with a commit hash in the URL) in the pull request description. For a new Ruby feature, check against CRuby's development branch.
* **Look for an existing helper** in JRuby or the Java standard library before writing one. Don't port C utilities like `atoi` or `memchr` by hand when Java already does the job. Search for code that already does what you need (`org.jruby.api.Create` for creating objects, for example).
* **Look for one central place** to make a change, as CRuby often does, before adding calls to many call sites.
* **Check whether the code belongs in a library.** Native and platform-specific logic often belongs in jnr-posix, jnr-ffi, jffi, jnr-enxio or jnr-unixsocket rather than in JRuby.
* **Find the cause of a failure** before you change code to make it pass. If a test from CRuby fails, decide first whether JRuby or the test is wrong.
* **Choose the right branch.** Most work targets `master`. A fix for a user-visible bug that also exists in the LTS release usually goes on `jruby-10.0`. Never open a pull request that only merges changes forward from `jruby-10.0` to `master`, or from `master` to `ruby-4.1`. Maintainers do that.

## Writing code

* Annotate every port from CRuby with `@CRuby`. Add `note` when the method ports only part of the C function, or when another Java method has the same annotation. Name the part in a few words (`note = "method redefinition check"`). A bare annotation on a partial port confuses reviewers.
* Split a long ported C function into smaller Java methods. The JVM optimizes small methods better.
* Use enums for fixed sets of values, records for compound keys, `VarHandle` for atomic fields, cached call sites instead of `callMethod`, and `Byte.toUnsignedInt` instead of `& 0xff`.
* Avoid new allocations on common paths, allocate for optional features only when they are active, and don't add fields to AST nodes. See "Writing Java for JRuby" in CONTRIBUTING.md.
* Match the signatures of neighboring overloads. Don't call `runtime.getCurrentContext()` just to reach a newer API.
* Keep lines short, one-line `if` statements included.
* Write short comments that say what the code does now and why. Don't narrate how the code used to work, what you changed, or what you tried first. Don't add comment blocks that repeat what the code says.
* Don't edit generated files. `RubyParser.java` and `RipperParser.java` come from `RubyParser.y` (see below).
* Stay in scope. If you notice something else worth fixing, note it for the person you work for instead of fixing it in the same change.

## Writing tests

Agents tend to put tests in `test/jruby` or `spec/regression`, because those files look easy to extend. Usually that is the wrong place:

1. Standard Ruby behavior goes in `spec/ruby` (`core`, `language` or `library`), as small mspec examples that match the style of the file around them. This is true even when the behavior is something JRuby never got right. Parsing and compilation behavior goes in `language`.
2. If an existing spec in `spec/ruby` now passes, remove its tag from `spec/tags` instead of writing a new test.
3. Only behavior specific to JRuby goes in `spec/jruby` (or `spec/java_integration`, `spec/compiler`).
4. `spec/regression` is only for unusual cases tied to one bug report.
5. Don't modify `test/mri`. Exclude failing tests in `test/mri/excludes` with a short reason. Don't update CRuby tests piece by piece, and don't fix a CRuby test locally without proposing the fix upstream.

Specs in `spec/ruby` must also pass on CRuby, because CI runs them there. Run them with CRuby's `ruby` as well as JRuby when you can, and add `ruby_version_is` guards for new behavior (see `spec/ruby/CONTRIBUTING.md`). Test through Ruby's public API, not JRuby internals.

For IR or compiler changes, add a case to `spec/compiler/general_spec.rb`, which runs each example in several execution modes, including an IR persistence round trip.

## Writing pull requests and comments

Many JRuby reviewers read English as a second language, and they read a lot of pull requests. Long, dense text slows them down and has drawn complaints in review.

* State what was wrong, what you changed and how you tested it. Stop there.
* Use plain words and short sentences. Avoid invented terms and jargon you haven't defined.
* Match the length to the change. A two-line fix needs two or three sentences.
* Don't claim things you haven't checked. For example, object references on a modern JVM are usually 4 bytes (compressed pointers), not 8. When you explain a speedup, test your explanation (the real cause can be simpler, like Java's compact strings).
* Back performance claims with the benchmark itself and numbers from before and after the change, as CONTRIBUTING.md describes.
* List anything you noticed but didn't fix, briefly, so a maintainer can decide whether to open an issue.
* Open the pull request as a draft if any work remains, and say when it is ready. Maintainers merge non-draft pull requests quickly and usually skip drafts.

## Before you commit

* Run `git status` and read the full diff. Remove anything you didn't mean to change: build output, copied directories, IDE files, unrelated formatting.
* Regenerate generated files instead of resolving conflicts in them by hand.
* Run the specs and tests closest to your change, in interpreter and JIT modes where it matters.
* When you address review comments, amend or drop the commits they apply to, so the history doesn't add code and then remove it.

## Building and testing in this checkout

These notes fill gaps in [BUILDING.md](BUILDING.md).

* **Rebuild `lib/jruby.jar` after changing Java code** with `./mvnw -q -o -DskipTests -pl core,shaded package`, as BUILDING.md describes. Drop `-o` if Maven needs to download something.
* **Bootstrap once before running Ruby tests**: `./mvnw -Pbootstrap -DskipTests`. Without it, the stdlib and gems that the tests need are missing. It is slow, so only run it when they are missing.
* **Run one spec file** with `bin/jruby spec/mspec/bin/mspec-run <path>`, or `bin/jruby spec/mspec/bin/mspec ci <path>` to skip tagged failures. Add `-T-Xjit.threshold=0` to compile everything with the JIT before it runs, which is how CI tests the JIT.
* **Run specs on CRuby** from `spec/ruby` with `ruby ../mspec/bin/mspec <path>`.
* **Run one CRuby test file** with `bin/jruby -Itest/tool/lib -Itest/mri/lib -Itest/mri <path> --excludes-dir=test/mri/excludes`. On a slow or busy machine, add `--timeout-scale=10`, because many tests start a new JRuby and time out otherwise.
* **Regenerate the parser** with `PATH=<dir with jay>:$PATH tool/generate_parser` after editing `RubyParser.y`. `jay` is not packaged anywhere, so build it from [jruby/jay](https://github.com/jruby/jay) (`cd src && make`).
* **Check that the JIT compiles your code** with `bin/jruby -rjruby -e 'JRuby.compile("...")'` for a snippet, or `bin/jruby -J-XX:-OmitStackTraceInFastThrow tool/jit_compile_check.rb <files>` for whole files.
* **Audit MRI excludes** with `bin/jruby tool/audit_mri_excludes <TestClass ...>`. It runs excluded tests offline and reports which now pass.
