# Performance guards

Benchmarks that fail CI when performance regresses.

```
bin/jruby -S rake test:perf_guard
JRUBY_OPTS=-Xjit.background=false bin/jruby bench/guard/runner.rb -v bench/guard/cases/braceless_hash_keywords.yml
```

Pass JRuby flags through `JRUBY_OPTS` rather than on the command line, so the
fresh JVM that confirms a regression gets them too.

## Writing a guard

A guard compares a *candidate* snippet against a *baseline* snippet that does
the same work by a path known to be fast. It fails when the candidate is
slower than the baseline by more than `max_ratio`:

```yaml
# https://github.com/jruby/jruby/issues/9814
prelude: |
  H = {}.freeze
max_ratio: 1.5
benchmarks:
  string key:
    baseline:  'H.merge({"a" => 1})'
    candidate: 'H.merge("a" => 1)'
```

- `prelude` runs once in the body of a fresh class. Use constants and
  methods, not local variables: each snippet is compiled into a method of
  that class, which runs it `batch` times (default 100) in a loop.
- `baseline` can be set once at the top level and shared.
- `max_ratio` (default 1.5) and `max_alloc_ratio` (bytes allocated, off by
  default) can be set per file or per benchmark.

Pick a baseline that differs from the candidate only in the property the bug
broke, like braces in the example above. When there's no natural pair,
compare against the simplest code that exercises the same operation.

Set `max_ratio` well between the ratio with and without the bug. Issue 9814
measured 2.8-4.7x with the bug and 1.0-1.2x without it, so 1.5 leaves room on
both sides. A guard catches cliffs, like code that stops being JIT-compiled,
an accidental quadratic loop or a deoptimization loop. It can't detect a 5%
slowdown on shared CI machines, and shouldn't try to.

## How a failure is decided

Comparing two snippets in the same JVM, instead of comparing against stored
timings, makes the result independent of the machine and its load. On top of
that:

1. **Thread CPU time.** Each sample measures the CPU time of the benchmark
   thread, which other processes competing for the machine don't inflate.
2. **Interleaved rounds.** Each round runs every snippet once, in a random
   order, and records candidate time / baseline time. Drift from thermal
   throttling, GC and background compilation hits both sides of a ratio.
3. **A confidence interval, not a mean.** After at least 8 rounds, a
   benchmark passes as soon as a 99% distribution-free confidence interval
   for the median ratio lies entirely below `max_ratio`, and becomes a
   suspect as soon as it lies entirely above. A single outlier sample can't
   move either end. One still undecided after 40 rounds is reported as
   inconclusive and doesn't fail the run.
4. **Suspects are re-measured three more times.** Early in a process the
   JIT is still compiling and recompiling shared core code, and one group can
   read 2-3x slow for a few seconds and then recover. So a suspect is
   measured again at the end of the run, then in a fresh JVM, twice. The run
   fails only if all four measurements agree.

To check for false alarms, the guard for issue 9814 was copied 100 times (800
benchmarks per run) and run against a build with the fix on a busy laptop:

- Without step 4, 6 runs, two at a time: 3 of 4,800 results were false
  regressions, all from one process within a few seconds of each other.
- With step 4, 9 runs, three at a time at a load average of 8-11: 0 of 7,200
  failed. One suspect (2.12x) didn't reproduce later in the same JVM, and 3
  were inconclusive.

Against a build without the fix, all 8 benchmarks fail at 2.8-4.8x, with and
without CI's C1-only `JAVA_OPTS`.

## Cost

A benchmark that clearly passes is decided after 8 rounds of about 5 ms per
snippet, after a 0.3 s warmup per file. In the runs above, 800 benchmarks took
80-110 s of wall time per JVM, so 1,000 benchmarks take about 2 minutes.
Re-measuring only happens for suspects, so it costs nothing when nothing has
regressed. To spread a large suite over several CI jobs, run each with
`--shard=I/N` (or `PERF_GUARD_SHARD=I/N`).

## Options

Run `bench/guard/runner.rb --help` for the full list. In rake, pass them with
`PERF_GUARD_OPTS`, for example `PERF_GUARD_OPTS=--verbose`. A failure prints
the random seed it used; `--seed=N` repeats the same snippet order.
