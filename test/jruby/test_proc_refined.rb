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

  module IntegerRefinement
    refine Integer do
      def double
        self * 2
      end
    end
  end

  # A second String#shout refinement with a different result, used to tell which refinement set is active.
  module StringRefinement2
    refine String do
      def shout
        downcase + "?"
      end
    end
  end

  # Refines operators so the specialized call paths (opt_plus / opt_lt / opt_aref equivalents) are exercised.
  module Operators
    refine Integer do
      def +(other) = "plus(#{self},#{other})"
      def <(other) = "lt"
    end
    refine Array do
      def [](i) = "at#{i}"
    end
  end

  # Refines Hash#[] to exercise the string-literal aref fast path (ArrayDerefInstr), which the builder emits
  # only in unrefined scopes and which otherwise dispatches straight to the builtin.
  module HashAref
    refine Hash do
      def [](k) = "aref(#{k})"
    end
  end

  class SuperBase
    def greet = "base"
  end

  # A refinement whose method calls super to reach the method it refines.
  module SuperModule
    refine SuperBase do
      def greet = "ref-" + super
    end
  end

  # `using` inside a refined proc's body is rejected: the memoized clone shares refined call sites across
  # every proc derived from the same (source, modules) key, which is only sound while all of them run under the
  # same refinement set.  A `using` mid-body would diverge that set and poison the shared clone.  The refinement
  # set is fixed at refined() time instead; pass every module in the single refined() call.  (MRI parity: the
  # same RuntimeError messages are raised by CRuby.)
  def test_using_rejected_in_body
    # main.using resolves to the top-level main object, so build these procs at the top level.
    mod = "TestProcRefined::StringRefinement"
    # main.using directly in the body
    pr = eval("->(){ using #{mod} }", TOPLEVEL_BINDING).refined(StringRefinement)
    assert_raise_with_message(RuntimeError, /main\.using is not permitted in a proc with refinements/) { pr.call }
    # main.using in a block nested inside the refined proc
    pr = eval("->(){ [1].each { using #{mod} } }", TOPLEVEL_BINDING).refined(StringRefinement)
    assert_raise_with_message(RuntimeError, /main\.using is not permitted in a proc with refinements/) { pr.call }
    # Module#using in a class/module body opened inside the refined proc (built at the top level so the enclosing
    # scope chain does not lexically pass through a method, which would trip the "not permitted in methods" guard).
    pr = eval("->(){ Class.new { using #{mod} } }", TOPLEVEL_BINDING).refined(StringRefinement)
    assert_raise_with_message(RuntimeError, /Module#using is not permitted in a proc with refinements/) { pr.call }
    # Module#using when the refined proc is run via module_eval (self is the module, but the block's scope is
    # still the clone, so the walk finds it).
    pr = eval("proc { using #{mod} }", TOPLEVEL_BINDING).refined(StringRefinement)
    assert_raise_with_message(RuntimeError, /Module#using is not permitted in a proc with refinements/) { Module.new.module_eval(&pr) }
  end

  # The refined proc is created from the receiver's real class: it must not inherit the receiver's
  # singleton state (CRuby behaves the same), while Proc subclasses are preserved.
  def test_singleton_state_not_inherited
    pr = ->(s) { s.shout }
    def pr.tag = :orig
    refined = pr.refined(StringRefinement)
    assert_equal(false, refined.respond_to?(:tag))
    assert_equal("HI!", refined.call("hi"))
    sub = Class.new(Proc)
    sub_refined = sub.new { |s| s.shout }.refined(StringRefinement)
    assert_equal(sub, sub_refined.class)
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

  # `refine` (which only defines methods on a refinement module, without activating them in the current scope)
  # does not touch the active refinement set, so unlike `using` it is allowed inside a refined proc's body.
  def test_refine_allowed_in_body
    refined = ->(s) {
      Module.new { refine(String) { def whisper = "w" } }
      s.shout
    }.refined(StringRefinement)
    assert_nothing_raised { assert_equal("HI!", refined.call("hi")) }
  end

  # class_eval gets its own copy of the refinement scope, so running a refined proc through it must not
  # pollute the memoized clone that later derivations reuse.
  def test_module_eval_does_not_leak_refinements
    body = proc { "ok".shout }.refined(StringRefinement)
    assert_equal("OK!", Class.new.class_eval(&body))
    again = proc { "ok".shout }.refined(StringRefinement)
    assert_equal("OK!", Class.new.class_eval(&again))
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

  # A /o (once) regexp literal interpolating a refined-method call is built under the refinement, and the clone's
  # once cache is independent of the source proc's.
  def test_once_regexp
    refined = ->(s) { /\A#{s.shout}\z/o }.refined(StringRefinement)
    r1 = refined.call("ab")
    assert_equal('\AAB!\z', r1.source)
    assert_same(r1, refined.call("zz")) # /o caches the first regexp on the clone's own once entry
    # the original proc has no refinement, so building the regexp raises
    assert_raise(NoMethodError) { ->(s) { /\A#{s.shout}\z/o }.call("ab") }
  end

  # A proc created lexically INSIDE a refined proc is not itself "a proc that already has refinements": it
  # only inherits the enclosing refinements lexically.  refined (and define_method) must therefore be
  # accepted on it, and the inner proc must see both the enclosing refinement and the one it adds.  Only the proc
  # handed back by refined is rejected for chaining.
  def test_nested_proc_inside_refined_proc_is_not_a_chain
    result = -> {
      inner = ->(s, n) { [s.shout, n.double] }
      inner.refined(StringRefinement).call("hi", 3)
    }.refined(IntegerRefinement).call
    # StringRefinement is added on the inner clone; IntegerRefinement is inherited from the enclosing refined proc.
    assert_equal ["HI!", 6], result

    # define_method from such a nested proc is allowed (it is not the refined result itself).
    assert_nothing_raised do
      -> { Class.new { define_method(:m, ->(s) { s }) } }.refined(IntegerRefinement).call
    end
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

  # The refined clone must work when reached through the block-yield path (each(&proc)), not just #call.
  def test_yield_path
    forwarded = []
    rr = ->(s) { forwarded << s.shout }.refined(StringRefinement)
    %w[p q].each(&rr)
    assert_equal(["P!", "Q!"], forwarded)
  end

  # The refinement must be carried when the clone is reached through the generic C invocation paths
  # (Method#call, Fiber, Thread) rather than a direct optimized Proc#call.
  def test_c_call_paths
    refined = ->(s) { s.shout }.refined(StringRefinement)
    assert_equal("A!", refined.send(:call, "a"))
    assert_equal("B!", refined.method(:call).call("b"))
    assert_equal("C!", Fiber.new(&refined).resume("c"))
    assert_equal("D!", Thread.new("d", &refined).value)
  end

  # Repeating the same (proc, modules) reuses the cached clone but stays correct; a different module set must
  # not return the previously cached clone, and switching back is still correct.
  def test_memoized
    orig = ->(s) { s.shout }
    assert_equal("HI!", orig.refined(StringRefinement).call("hi"))
    assert_equal("HI!", orig.refined(StringRefinement).call("hi"))
    assert_equal("hi?", orig.refined(StringRefinement2).call("hi"))
    assert_equal("HI!", orig.refined(StringRefinement).call("hi"))
  end

  # Run the block with performance warnings enabled and $stderr captured; return the captured output.
  def capture_performance_warnings
    require 'stringio'
    old = Warning[:performance]
    Warning[:performance] = true
    stderr, $stderr = $stderr, StringIO.new
    begin
      yield
      $stderr.string
    ensure
      $stderr = stderr
      Warning[:performance] = old
    end
  end

  # A differing module set overwrites the single memo slot and emits a performance-category
  # warning, as in CRuby.
  def test_memo_different_modules_warning
    orig = ->(s) { s.shout }
    orig.refined(StringRefinement).call("hi")
    out = capture_performance_warnings do
      orig.refined(StringRefinement2).call("hi")
    end
    assert_match(/Proc#refined called with different modules for the same block disables memoization/, out)
  end

  # Proc#ruby2_keywords marks the shared block scope, possibly after a clone was memoized.  The stale
  # memo is rebuilt (with a warning naming the cause) rather than reused or mutated: the new proc
  # delegates keywords like its source, while procs built before the mark keep their creation-time
  # behavior.  Mirrors CRuby's test_refined_ruby2_keywords_memo.
  def test_memo_ruby2_keywords
    target = ->(a, k: nil) { [a, k] }
    pr = proc { |*args| target.call(*args) }
    q1 = pr.refined(StringRefinement) # memoize a clone before the mark
    pr.ruby2_keywords
    assert_equal([1, 2], pr.call(1, k: 2))
    out = capture_performance_warnings do
      q2 = pr.refined(StringRefinement)
      assert_equal([1, 2], q2.call(1, k: 2))
      # the clone made before the mark is not retroactively changed
      assert_raise(ArgumentError) { q1.call(1, k: 2) }
      # the rebuilt memo is hit from now on; no further warnings
      assert_equal([1, 2], pr.refined(StringRefinement).call(1, k: 2))
    end
    assert_match(/Proc#refined re-copies the block because the ruby2_keywords flag changed after the copy was memoized/, out)
    assert_equal(1, out.scan(/ruby2_keywords flag changed/).size)
  end

  # Procs sharing the same source block but capturing different closure environments hit the same cache slot
  # (the environment is not part of the key), yet each clone must keep its own captured environment.
  def test_memo_distinct_environments
    factory = ->(tag) { ->(s) { "#{tag}:#{s.shout}" } }
    p1 = factory.call("A")
    p2 = factory.call("B")
    r1 = p1.refined(StringRefinement)
    r2 = p2.refined(StringRefinement)
    assert_equal("A:X!", r1.call("x"))
    assert_equal("B:Y!", r2.call("y"))
    assert_equal("A:X!", r1.call("x"))
  end

  # The clone must reproduce the begin/rescue/ensure control flow (copied exception regions, shared locals).
  def test_rescue_ensure
    refined = ->(s) {
      r = nil
      begin
        r = s.shout
      rescue NoMethodError
        r = "rescued"
      ensure
        r = "#{r}."
      end
      r
    }.refined(StringRefinement)
    assert_equal("YO!.", refined.call("yo"))
  end

  # Keyword and optional arguments must be reproduced on the clone.
  def test_keyword_and_optional_args
    refined = ->(a, b = "z", c:, d: "w") {
      [a, b, c, d].map { |x| x.shout }.join
    }.refined(StringRefinement)
    assert_equal("A!Z!C!W!", refined.call("a", c: "c"))
    assert_equal("A!B!C!D!", refined.call("a", "b", c: "c", d: "d"))
  end

  # Literal when-clauses compile to a jump table that must survive the clone.
  def test_case_when_literal
    refined = ->(s) {
      case s.shout
      when "A!" then 1
      when "B!" then 2
      else 0
      end
    }.refined(StringRefinement)
    assert_equal(1, refined.call("a"))
    assert_equal(2, refined.call("b"))
    assert_equal(0, refined.call("c"))
  end

  # A flip-flop keeps state keyed off the local scope; the clone must run its own flip state, and a second call
  # starts from a fresh state.
  def test_flip_flop
    body = ->(arr) {
      out = []
      arr.each { |i| out << i if (i == 2)..(i == 4) }
      out
    }
    refined = body.refined(StringRefinement)
    assert_equal([2, 3, 4], refined.call([1, 2, 3, 4, 5]))
    assert_equal([2, 3, 4], refined.call([1, 2, 3, 4, 5]))
  end

  # The clone reports the same source location, parameters and arity as the source proc.
  def test_preserves_location_and_parameters
    orig = ->(a, b = 1, *c, d:, **e, &f) { a }
    refined = orig.refined(StringRefinement)
    assert_equal(orig.source_location, refined.source_location)
    assert_equal(orig.parameters, refined.parameters)
    assert_equal(orig.arity, refined.arity)
  end

  # Operator refinements must apply on the clone (specialized call paths) without leaking into the originals.
  def test_operators
    refined = ->(a, b) { [a + b, a < b] }.refined(Operators)
    assert_equal(["plus(1,2)", "lt"], refined.call(1, 2))
    aref = ->(a) { a[0] }.refined(Operators)
    assert_equal("at0", aref.call([9]))
    assert_equal(3, ->(a, b) { a + b }.call(1, 2))
  end

  # A string-literal Hash aref compiles to the optimized ArrayDerefInstr, which bypasses refinements; the
  # Proc#refined clone must fall back to a normal refined call so the refinement still applies (matching CRuby
  # and a fresh build under `using`).
  def test_optimized_aref
    refined = ->(h) { h["x"] }.refined(HashAref)
    assert_equal("aref(x)", refined.call({ "x" => 1 }))
    # the original proc keeps the builtin fast path
    assert_equal(1, ->(h) { h["x"] }.call({ "x" => 1 }))
  end

  # A refined method may call super to reach the method it refines.
  def test_super
    refined = ->(o) { o.greet }.refined(SuperModule)
    assert_equal("ref-base", refined.call(SuperBase.new))
  end

  # The clone shares the source proc's environment, so a recursive call through a captured local reaches the
  # refined clone again and keeps the refinements.
  def test_recursion_sees_refinements
    fact = nil
    fact = ->(s) { s.empty? ? "" : s[0].shout + fact.call(s[1..]) }.refined(StringRefinement)
    assert_equal("A!B!C!", fact.call("abc"))
  end

  # Many independently created refined clones must all survive GC and keep working.
  def test_gc
    procs = 100.times.map { ->(s) { s.shout }.refined(StringRefinement) }
    JRuby.gc rescue GC.start
    procs.each { |pr| assert_equal("A!", pr.call("a")) }
  end

  # A Symbol#to_proc (&:shout) evaluated inside a refined clone resolves the method under the clone's refinements.
  # A baked &:sym caches a non-refined symbol proc; the clone rewrites it to a plain symbol so the (now refined)
  # call site converts it through Symbol#toRefinedProc with the clone's refinement scope.
  def test_symbol_to_proc_sees_refinements
    refined = ->(arr) { arr.map(&:shout) }.refined(StringRefinement)
    assert_equal(["A!", "B!"], refined.call(["a", "b"]))
    # the original proc has no refinement, so the symbol proc raises
    assert_raise(NoMethodError) { ->(arr) { arr.map(&:shout) }.call(["a"]) }
  end

  # A literal `def` inside the block captures the clone's refinements (like a def inside a `using` scope), so the
  # refinement also applies when that method is called later.  Covers both a singleton def and an instance def.
  def test_def_in_block_sees_refinements
    refined = ->(s) {
      o = Object.new
      def o.m = "hi".shout
      [s.shout, o.m]
    }.refined(StringRefinement)
    assert_equal(["YO!", "HI!"], refined.call("yo"))

    inst = ->(s) {
      k = Class.new { def m; "hi".shout; end }
      [s.shout, k.new.m]
    }.refined(StringRefinement)
    assert_equal(["YO!", "HI!"], inst.call("yo"))

    # the original proc's def must NOT see refinements
    assert_raise(NoMethodError) do
      ->(s) { o = Object.new; def o.m = "hi".shout; o.m }.call("yo")
    end
  end

  # A class/module (re)opened with a `class`/`module` keyword inside a refined proc -- as opposed to via a
  # class_eval/Class.new block -- compiles to its own class-body scope, which must also become refinement-aware
  # so the body and any def inside it see the refinements (matching a body opened under a `using` scope).
  #
  # The `class`/`module` keyword is a syntax error inside a method body (even within a block there), so these
  # procs are built at class-body scope (block bodies, which the keyword is legal in) rather than inside the
  # test methods.
  class ReopenTarget; end
  module ReopenModule; end

  # reopen an existing class
  REOPEN_CLASS = ->(s) {
    class ReopenTarget
      def m = "hi".shout
    end
    [s.shout, ReopenTarget.new.m]
  }
  # define a brand-new class with a superclass, plus a module method, plus class << self
  REOPEN_VARIANTS = ->(s) {
    class ReopenSub < ReopenTarget
      def m = "hi".shout
    end
    module ReopenModule
      def self.m = "hi".shout
    end
    class ReopenTarget
      class << self
        def n = "hi".shout
      end
    end
    [s.shout, ReopenSub.new.m, ReopenModule.m, ReopenTarget.n]
  }
  # an otherwise identical proc whose class body must NOT see refinements when called without refined
  REOPEN_UNREFINED = ->(s) {
    class ReopenTarget
      def unrefined = "hi".shout
    end
    ReopenTarget.new.unrefined
  }

  def test_class_keyword_body_sees_refinements
    refined = REOPEN_CLASS.refined(StringRefinement)
    assert_equal(["YO!", "HI!"], refined.call("yo"))

    variants = REOPEN_VARIANTS.refined(StringRefinement)
    assert_equal(["YO!", "HI!", "HI!", "HI!"], variants.call("yo"))

    # the original proc's class body must NOT see refinements
    assert_raise(NoMethodError) { REOPEN_UNREFINED.call("yo") }
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
