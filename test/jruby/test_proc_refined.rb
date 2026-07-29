require 'test/unit'
require 'test/jruby/test_helper'
require 'tempfile'
require 'jruby'

class TestProcRefined < Test::Unit::TestCase
  include TestHelper
  module StringRefinement
    refine String do
      def shout
        upcase + "!"
      end
    end
  end

  # Instructions of a refined clone are copied on the first call: nested closure shells only
  # appear once the enclosing body materializes, so a never-called refined proc stays a cheap shell.
  def test_clone_materializes_on_first_call
    prc = ->(a) { a.map { |s| s.shout } }
    refined = prc.refined(StringRefinement)
    scope = JRuby.reference(refined).get_block.get_body.get_scope
    assert_equal 0, scope.get_closures.size, "nested shells created before the first call"
    assert_equal ["A!"], refined.call(["a"])
    assert_equal 1, scope.get_closures.size, "nested shell not created at materialization"
  end

  # Concurrent first calls can force the lazy instruction copy from several threads at once;
  # a duplicate build would register nested shells twice in the clone.
  def test_concurrent_first_call_builds_once
    20.times do
      refined = ->(s) { [s].map { |x| x.shout }.first }.refined(StringRefinement)
      scope = JRuby.reference(refined).get_block.get_body.get_scope
      latch = java.util.concurrent.CountDownLatch.new(1)
      results = Queue.new
      threads = Array.new(8) do
        Thread.new do
          latch.await
          results << refined.call("a")
        end
      end
      latch.count_down
      threads.each(&:join)
      8.times { assert_equal "A!", results.pop }
      assert_equal 1, scope.get_closures.size, "duplicate lazy build registered nested shells twice"
    end
  end

  # p.refined(a).refined(b) shares one memoized clone with p.refined(a, b); the memo is written at
  # materialization, so the never-called chain intermediate does not evict the live entry.
  def test_chain_shares_memoized_clone
    quiet = Module.new { refine(String) { def shout; downcase; end } }
    prc = ->(s) { s.shout }
    combined = prc.refined(StringRefinement, quiet)
    assert_equal "hi", combined.call("Hi")
    chained = prc.refined(StringRefinement).refined(quiet)
    assert_equal "hi", chained.call("Hi")
    combined_scope = JRuby.reference(combined).get_block.get_body.get_scope
    chained_scope = JRuby.reference(chained).get_block.get_body.get_scope
    # not assert_equal: test-unit's diagnostics call encoding, which IRScope stubs with an exception
    assert_same combined_scope, chained_scope, "chained call did not reuse the memoized clone"
  end

  # A proc created inside a refined body lives in the clone tree; refined() on it must still produce
  # a proper root clone: memoized once materialized, chainable, rejected by define_method.
  def test_refined_on_proc_from_refined_body
    quiet = Module.new { refine(String) { def shout; downcase; end } }
    inner = -> { ->(s) { s.shout } }.refined(StringRefinement).call
    r1 = inner.refined(quiet)
    assert_equal "hi", r1.call("Hi")
    r2 = inner.refined(quiet)
    assert_same JRuby.reference(r1).get_block.get_body.get_scope,
                JRuby.reference(r2).get_block.get_body.get_scope,
                "repeat refined() on a clone-tree proc did not hit the memo"
    # the chain re-clones from inner with the merged list, so the later module wins
    assert_equal "HI!", r1.refined(StringRefinement).call("Hi")
    assert_raise(ArgumentError) { Class.new { define_method(:m, r1) } }
  end

  # Refinement activation (refined -> usingModuleRecursive) can run on any thread while another
  # thread mutates the same module's refinement map via Module#refine; the activation must
  # snapshot the map instead of iterating it unsynchronized (ConcurrentModificationException).
  def test_concurrent_refined_and_refine
    mod = Module.new { refine(String) { def shout = upcase } }
    fillers = Array.new(2) { Module.new { refine(String) { def noop = self } } }
    src = ->(s) { s.shout }
    writer = Thread.new do
      300.times { mod.module_eval { refine(Class.new) { def x = 1 } } }
    end
    i = 0
    assert_nothing_raised do
      while writer.alive?
        # alternate the second module to defeat the single-slot memo and force re-activation
        assert_equal("HI", src.refined(mod, fillers[i % 2]).call("hi"))
        i += 1
      end
      assert_equal("HI", src.refined(mod, fillers[0]).call("hi"))
    end
  ensure
    writer.join
  end

  # Each refined clone is grafted under the SOURCE proc's (shared, long-lived) lexical parent, but it must
  # not be registered in that parent's nested-closure / lexical-child lists: doing so would retain every discarded
  # clone forever (memory leak) and race with concurrent calls on those unsynchronized/AOT lists.  Drive many
  # cache-missing clones (distinct module each time) and assert the parent's lists do not grow.
  def test_clones_not_retained_by_lexical_parent
    prc = ->(s) { s.to_s }
    closure = JRuby.reference(prc).get_block.get_body.get_scope
    parent = closure.get_lexical_parent
    before_closures = parent.get_closures.size
    before_children = parent.get_lexical_scopes.size
    200.times do
      mod = Module.new { refine(String) { def to_s; "x"; end } }
      prc.refined(mod).call("hi")
    end
    assert_operator parent.get_closures.size - before_closures, :<=, 1,
                    "refined clones accumulated in the source's parent nestedClosures"
    assert_operator parent.get_lexical_scopes.size - before_children, :<=, 1,
                    "refined clones accumulated in the source's parent lexicalChildren"
  end

  # An exception raised through a refined clone whose source block was created at top level (so the captured
  # frame has no method name) must still build a backtrace -- the clone always runs interpreted, and a null frame name
  # used to crash backtrace construction with a Java NullPointerException instead of surfacing the Ruby exception.
  def test_exception_backtrace_through_top_level_clone
    refined = -> { raise "boom" }.refined(StringRefinement)
    err = assert_raise(RuntimeError) { refined.call }
    assert_equal "boom", err.message
    assert_match(/block in /, err.backtrace.first)
  end

  # Many independently created refined clones must all survive GC and keep working.
  def test_gc
    procs = 100.times.map { ->(s) { s.shout }.refined(StringRefinement) }
    JRuby.gc rescue GC.start
    procs.each { |pr| assert_equal("A!", pr.call("a")) }
  end

  # A refinement-aware clone is grafted under an already-built enclosing scope, so it needs its own full IR
  # built before it can be JIT-compiled.  Run in a subprocess with a low JIT threshold and synchronous (non
  # background) compilation so both the original and the clone cross the threshold deterministically.  We
  # assert two things: the refinement still produces correct results after JIT (and the original is
  # unaffected), and -- crucially for guarding the fix -- that the clone did not hit "JIT failed".  Without
  # the fix the clone cannot build its full IR and falls back to the interpreter, which is still correct, so
  # the JIT log is what distinguishes a working fix from a silent regression.
  def test_refinement_survives_jit
    script = <<~'RUBY'
      module R
        refine(String) { def upcase; "REFINED"; end }
      end
      prc = ->(s) { s.upcase }
      1000.times { exit(1) unless prc.call("hi") == "HI" }       # JIT the original (bare upcase) first
      refined = prc.refined(R)
      1000.times { exit(2) unless refined.call("hi") == "REFINED" } # drive the clone past the threshold
      exit(3) unless prc.call("hi") == "HI"                      # original must stay unaffected post-JIT
      print "OK"
    RUBY
    Tempfile.create(['jit_refine', '.rb']) do |f|
      f.write(script)
      f.flush
      out = jruby("-Xjit.threshold=10 -Xjit.background=false -Xjit.logging #{f.path} 2>&1")
      assert_include out, "OK", "refinement-aware proc produced wrong results under JIT"
      assert_not_include out, "JIT failed", "refinement-aware clone could not be JIT-compiled"
    end
  end
end
