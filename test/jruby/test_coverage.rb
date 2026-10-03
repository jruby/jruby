require 'test/unit'
require 'coverage'
require 'tmpdir'
require_relative 'test_helper'

class TestCoverage < Test::Unit::TestCase
  include TestHelper

  def teardown
    Coverage.result if Coverage.state != :idle
    # each test loads METHODS again; drop the classes so definitions do not pile up across tests
    [:Sub, :Prepended, :Mixin, :Covered, :Branchy].each { |c| Object.send(:remove_const, c) if Object.const_defined?(c, false) }
  end

  def test_coverage_handles_null_filename # jruby/jruby#5099
    Coverage.start
    JRuby.runtime.executeScript('1 + 1', nil)
    assert_nothing_raised { Coverage.result }
  end

  # The MRI suite covers the basics of method coverage. These tests cover what is specific to JRuby: the
  # execution engines, real parallelism, and the ways a method entry can be created.

  METHODS = <<~'RUBY'
    class Covered
      def plain(a, b = 1, *c, d:, **e, &f); end
      define_method(:block) { |x| x }
      define_method(:lam, &->(x) { x })
      alias aliased plain
      def self.single; end
      class << self
        def single2; end
      end
      def strict(a); end
      def keyword(a:); end
      def default(a = raise("boom")); end
      def multi(a,
                b)
      end
    end
    module Mixin
      def mixed; end
      module_function :mixed
    end
    class Prepended
      prepend Module.new
      def prepended; end
    end
    class Sub < Covered
      private :plain
    end
  RUBY

  def test_method_coverage_keys_and_counts
    with_source(METHODS) do |path|
      Coverage.start(methods: true)
      load path
      c = Covered.new
      c.plain(1, d: 2)
      c.block(1)
      c.lam(1)
      2.times { c.aliased(1, d: 2) }
      Covered.single
      Covered.single2
      Mixin.mixed
      Prepended.new.prepended
      Sub.new.send(:plain, 1, d: 2)
      methods = Coverage.result[path][:methods]

      expected = {
        key(Covered, :plain, 2, 'def', 'end') => 4,
        key(Covered, :block, 3, '{', '}') => 1,
        key(Covered, :lam, 4, '(x)', '}') => 1,
        key(Covered.singleton_class, :single, 6, 'def', 'end') => 1,
        key(Covered.singleton_class, :single2, 8, 'def', 'end') => 1,
        key(Covered, :strict, 10, 'def', 'end') => 0,
        key(Covered, :keyword, 11, 'def', 'end') => 0,
        key(Covered, :default, 12, 'def', 'end') => 0,
        [Covered, :multi, 13, 2, 15, 5] => 0,
        key(Mixin, :mixed, 18, 'def', 'end') => 0,
        key(Mixin.singleton_class, :mixed, 18, 'def', 'end') => 1,
        key(Prepended, :prepended, 23, 'def', 'end') => 1,
      }
      assert_equal expected, methods
    end
  end

  EVALED = <<~'RUBY'
    class Covered
      class_eval <<-INNER, __FILE__, __LINE__ + 1
        def from_eval(x); x; end
      INNER
    end
  RUBY

  # An eval that is not covered still counts the calls of the methods it defines, as in MRI. Only its lines are
  # left out (see Coverage.setup's eval option).
  def test_method_coverage_counts_methods_defined_by_an_eval
    with_source(EVALED) do |path|
      Coverage.start(methods: true)
      load path
      3.times { Covered.new.from_eval(1) }
      assert_equal 3, Coverage.result[path][:methods][key(Covered, :from_eval, 3, 'def', 'end')]
    end
  end

  COMMAND_BLOCKS = <<~'RUBY'
    class Covered
      def self.Make(&b) = b
      define_method (:brace) { |x| x }
      define_method (
        :do_block
      ) do |x|
        x
      end
    end
    made = Covered::Make { |x| x }
    Covered.define_method(:constant_call, &made)
  RUBY

  # A block that follows an argument list, or a call of a method named like a constant, comes through its own
  # grammar rule. Each of those rules has to record where the block starts and ends.
  def test_method_coverage_spans_blocks_that_follow_command_arguments
    with_source(COMMAND_BLOCKS) do |path|
      Coverage.start(methods: true)
      load path
      c = Covered.new
      c.brace(1)
      c.do_block(1)
      c.constant_call(1)
      methods = Coverage.result[path][:methods]
      assert_equal 1, methods[key(Covered, :brace, 3, '{', '}')]
      assert_equal 1, methods[[Covered, :do_block, 6, source_line(6).index('do'), 8, 5]]
      assert_equal 1, methods[key(Covered, :constant_call, 10, '{', '}')]
    end
  end

  def test_method_coverage_counts_only_calls_that_get_past_argument_processing
    with_source(METHODS) do |path|
      Coverage.start(methods: true)
      load path
      c = Covered.new
      assert_raise(ArgumentError) { c.strict(1, 2) }
      assert_raise(ArgumentError) { c.keyword }
      assert_raise(RuntimeError) { c.default }
      assert_raise(ArgumentError) { c.block(1, 2) }
      assert_raise(ArgumentError) { c.lam }
      methods = Coverage.peek_result[path][:methods]
      assert_equal 0, methods[key(Covered, :strict, 10, 'def', 'end')]
      assert_equal 0, methods[key(Covered, :keyword, 11, 'def', 'end')]
      assert_equal 0, methods[key(Covered, :default, 12, 'def', 'end')]
      assert_equal 0, methods[key(Covered, :block, 3, '{', '}')]
      assert_equal 0, methods[key(Covered, :lam, 4, '(x)', '}')]

      c.strict(1)
      c.keyword(a: 1)
      c.default(1)
      c.block(1)
      c.lam(1)
      methods = Coverage.result[path][:methods]
      assert_equal 1, methods[key(Covered, :strict, 10, 'def', 'end')]
      assert_equal 1, methods[key(Covered, :keyword, 11, 'def', 'end')]
      assert_equal 1, methods[key(Covered, :default, 12, 'def', 'end')]
      assert_equal 1, methods[key(Covered, :block, 3, '{', '}')]
      assert_equal 1, methods[key(Covered, :lam, 4, '(x)', '}')]
    end
  end

  def test_method_coverage_leaves_nothing_behind_when_a_define_method_call_fails
    source = "class Covered\n  BLK = proc { |a| a }\n  define_method(:blk, &BLK)\nend\n"
    with_source(source) do |path|
      Coverage.start(methods: true)
      load path
      assert_raise(ArgumentError) { Covered.new.blk }
      Covered::BLK.call(1) # the same block run as a plain block; not a call of the method
      assert_equal 0, Coverage.result[path][:methods][key(Covered, :blk, 2, '{', '}')]
    end
  end

  JAVA_SUBCLASSES = <<~'RUBY'
    class Covered < java.util.ArrayList
      def initialize(x)
        super()
        @x = x
      end
    end
    class Sub < java.util.ArrayList
      def initialize(x)
        super(x)
      end
    end
  RUBY

  # The initialize of a Java subclass runs through the split-constructor path, or is skipped when it is a plain
  # super. It does not go through a regular method call.
  def test_method_coverage_counts_java_subclass_initialize
    with_source(JAVA_SUBCLASSES) do |path|
      Coverage.start(methods: true)
      load path
      # Sub#initialize is a plain forwarding super: the body is skipped and coverElidedCall counts the call.
      # Check we take that path, since the counts below pass either way. Must run before the first Sub.new,
      # which replaces the entry with the Java constructor wrapper.
      assert JRuby.reference(Sub).searchMethod('initialize').getJavaConstructorContext.directSuperForwardable(1)

      3.times { Covered.new(1) }
      3.times { Sub.new(1) }
      methods = Coverage.result[path][:methods]
      assert_equal 3, methods[[Covered, :initialize, 2, 2, 5, 5]]
      assert_equal 3, methods[[Sub, :initialize, 8, 2, 10, 5]]
    end
  end

  def test_method_coverage_follows_suspend_resume_and_clear
    with_source(METHODS) do |path|
      Coverage.setup(methods: true)
      load path
      c = Covered.new
      c.strict(1)                                 # set up but not running: not counted
      Coverage.resume
      c.strict(1)
      Coverage.suspend
      c.strict(1)                                 # suspended: not counted
      assert_equal 1, Coverage.peek_result[path][:methods][key(Covered, :strict, 10, 'def', 'end')]
      Coverage.resume
      c.strict(1)
      assert_equal 2, Coverage.result(stop: false, clear: true)[path][:methods][key(Covered, :strict, 10, 'def', 'end')]
      assert_equal 0, Coverage.peek_result[path][:methods][key(Covered, :strict, 10, 'def', 'end')]
      c.strict(1)
      assert_equal 1, Coverage.result[path][:methods][key(Covered, :strict, 10, 'def', 'end')]
      assert_equal :idle, Coverage.state
    end
  end

  # define_method(name, method_object) binds a copy that keeps its own name. MRI keys the entry by that name (the
  # method's original_id), not the name the copy was defined under.
  def test_method_coverage_keys_define_method_from_method_object_by_its_original_name
    source = "class Covered\n  def original; end\nend\nclass Sub < Covered\n  define_method(:renamed, instance_method(:original))\nend\n"
    with_source(source) do |path|
      Coverage.start(methods: true)
      load path
      Sub.new.renamed
      methods = Coverage.result[path][:methods]
      assert_equal 1, methods[key(Sub, :original, 2, 'def', 'end')]
      assert_nil methods[key(Sub, :renamed, 2, 'def', 'end')]
      assert_equal 0, methods[key(Covered, :original, 2, 'def', 'end')]
    end
  end

  # Once measurement stops nothing stays instrumented: the counter is detached, so later calls do not pay for it.
  def test_method_coverage_detaches_counters_when_measurement_stops
    with_source("class Covered\n  def ping; end\nend\n") do |path|
      Coverage.start(methods: true)
      load path
      entry = JRuby.reference(Covered).searchMethod('ping')
      Covered.new.ping
      assert_not_nil entry.getMethodCoverage

      Coverage.result
      assert_nil entry.getMethodCoverage

      # A later run measures the file again from scratch rather than counting into the discarded counter.
      Coverage.start(methods: true)
      load path
      Covered.new.ping
      assert_equal 1, Coverage.result[path][:methods][key(Covered, :ping, 2, 'def', 'end')]
    end
  end

  def test_method_coverage_counts_are_exact_under_parallel_calls
    source = "def hot(x); x; end\nObject.send(:define_method, :hot_block) { |x| x }\n"
    with_source(source) do |path|
      Coverage.start(methods: true)
      load path
      threads, calls = 8, 5000
      threads.times.map { Thread.new { calls.times { |i| hot(i); hot_block(i) } } }.each(&:join)
      methods = Coverage.result[path][:methods]
      assert_equal threads * calls, methods[key(Object, :hot, 1, 'def', 'end')]
      assert_equal threads * calls, methods[key(Object, :hot_block, 2, '{', '}')]
    end
  end

  def test_method_coverage_result_shape_per_mode
    with_source("def shape; end\n") do |path|
      Coverage.start(methods: true)
      load path
      assert_equal({ methods: { key(Object, :shape, 1, 'def', 'end') => 0 } }, Coverage.result[path])

      Coverage.start(lines: true, methods: true)
      load path
      assert_equal({ lines: [1], methods: { key(Object, :shape, 1, 'def', 'end') => 0 } }, Coverage.result[path])

      Coverage.start(oneshot_lines: true, methods: true)
      load path
      assert_equal({ oneshot_lines: [1], methods: { key(Object, :shape, 1, 'def', 'end') => 0 } }, Coverage.result[path])

      Coverage.start(:all)
      load path
      assert_equal [:lines, :branches, :methods], Coverage.result[path].keys
    end
  end

  def test_method_coverage_across_execution_modes
    with_source(METHODS) do |path|
      script = File.join(File.dirname(path), 'driver.rb')
      File.write(script, <<~RUBY)
        require 'coverage'
        Coverage.start(methods: true)
        load #{path.inspect}
        c = Covered.new
        3.times { c.plain(1, d: 2); c.block(1); c.lam(1); c.aliased(1, d: 2); Covered.single; Mixin.mixed; Prepended.new.prepended }
        c.strict(1, 2) rescue nil
        Coverage.result[#{path.inspect}][:methods].sort_by { |k, _| k.drop(1) }.each { |k, v| puts [k[0], *k[1..]].join(' ') + " => " + v.to_s }
      RUBY

      expected = jruby(script).lines
      assert_equal 12, expected.size, expected.join
      assert_include expected, "Covered plain #{span(2, 'def', 'end')} => 6\n"
      assert_include expected, "Covered strict #{span(10, 'def', 'end')} => 0\n"
      assert_include expected, "#<Class:Mixin> mixed #{span(18, 'def', 'end')} => 3\n"

      ['-X-C', '-X+C', '-Xjit.threshold=0 -Xjit.background=false',
       '-Xcompile.invokedynamic=true -Xjit.threshold=0 -Xjit.background=false'].each do |flags|
        assert_equal expected, jruby("#{flags} #{script}").lines, flags
      end
    end
  end

  # Branch coverage. The expected result below is what CRuby reports for the same file and calls.

  BRANCHES = <<~'RUBY'
    class Branchy
      def classify(x)
        if x > 0
          :positive
        elsif x < 0
          :negative
        else
          :zero
        end
      end

      def describe(x)
        kind = x.zero? ? :zero : :nonzero
        kind = :big unless x < 100
        case x
        when 0 then :none
        when 1, 2
          :few
        else
          :many
        end
      end

      def count_down(x)
        while x > 0
          x -= 1
        end
        x += 1 until x > 2
        x
      end

      def safe(x)
        x&.abs
      end

      def match(x)
        case x
        in Integer => n if n > 10 then :large
        in Integer
          :small
        end
      end
    end
  RUBY

  EXPECTED_BRANCHES = {
        [:if, 0, 3, 4, 9, 7] => {
          [:then, 1, 4, 6, 4, 15] => 1,
          [:else, 2, 5, 4, 9, 7] => 2,
        },
        [:if, 3, 5, 4, 9, 7] => {
          [:then, 4, 6, 6, 6, 15] => 1,
          [:else, 5, 8, 6, 8, 11] => 1,
        },
        [:if, 6, 13, 11, 13, 37] => {
          [:then, 7, 13, 21, 13, 26] => 1,
          [:else, 8, 13, 29, 13, 37] => 2,
        },
        [:unless, 9, 14, 4, 14, 30] => {
          [:else, 10, 14, 4, 14, 30] => 2,
          [:then, 11, 14, 4, 14, 15] => 1,
        },
        [:case, 12, 15, 4, 21, 7] => {
          [:when, 13, 16, 16, 16, 21] => 1,
          [:when, 14, 18, 6, 18, 10] => 1,
          [:else, 15, 20, 6, 20, 11] => 1,
        },
        [:while, 16, 25, 4, 27, 7] => {
          [:body, 17, 26, 6, 26, 12] => 3,
        },
        [:until, 18, 28, 4, 28, 22] => {
          [:body, 19, 28, 4, 28, 10] => 3,
        },
        [:"&.", 20, 33, 4, 33, 10] => {
          [:then, 21, 33, 4, 33, 10] => 1,
          [:else, 22, 33, 4, 33, 10] => 1,
        },
        [:case, 23, 37, 4, 41, 7] => {
          [:in, 24, 38, 35, 38, 41] => 1,
          [:in, 25, 40, 6, 40, 12] => 1,
          [:else, 26, 37, 4, 41, 7] => 0,
        },
  }

  def exercise_branches
    b = Branchy.new
    b.classify(1); b.classify(-1); b.classify(0)
    b.describe(0); b.describe(1); b.describe(200)
    b.count_down(3)
    b.safe(nil); b.safe(-2)
    b.match(20); b.match(3)
  end

  def test_branch_coverage_keys_and_counts
    with_source(BRANCHES) do |path|
      Coverage.start(branches: true)
      load path
      exercise_branches
      assert_equal EXPECTED_BRANCHES, Coverage.result[path][:branches]
    end
  end

  def test_branch_coverage_result_shape_per_mode
    with_source("x = 1\ny = x > 0 ? :pos : :neg\n") do |path|
      Coverage.start(branches: true)
      load path
      result = Coverage.result[path]
      assert_equal [:branches], result.keys
      assert_equal({ [:if, 0, 2, 4, 2, 23] => { [:then, 1, 2, 12, 2, 16] => 1, [:else, 2, 2, 19, 2, 23] => 0 } }, result[:branches])

      Coverage.start(:all)
      load path
      result = Coverage.result[path]
      assert_equal [:lines, :branches, :methods], result.keys
      assert_equal 1, result[:branches].size
    end
  end

  def test_branch_coverage_follows_suspend_resume_and_clear
    with_source(BRANCHES) do |path|
      Coverage.setup(branches: true)
      load path
      b = Branchy.new
      key = [:if, 0, 3, 4, 9, 7]
      b.classify(1)                                # set up but not running: not counted
      Coverage.resume
      b.classify(1)
      Coverage.suspend
      b.classify(1)                                # suspended: not counted
      assert_equal({ [:then, 1, 4, 6, 4, 15] => 1, [:else, 2, 5, 4, 9, 7] => 0 }, Coverage.peek_result[path][:branches][key])
      Coverage.resume
      b.classify(-1)
      assert_equal({ [:then, 1, 4, 6, 4, 15] => 1, [:else, 2, 5, 4, 9, 7] => 1 }, Coverage.result(stop: false, clear: true)[path][:branches][key])
      assert_equal({ [:then, 1, 4, 6, 4, 15] => 0, [:else, 2, 5, 4, 9, 7] => 0 }, Coverage.peek_result[path][:branches][key])
      b.classify(0)
      assert_equal({ [:then, 1, 4, 6, 4, 15] => 0, [:else, 2, 5, 4, 9, 7] => 1 }, Coverage.result[path][:branches][key])
      assert_equal :idle, Coverage.state
    end
  end

  def test_branch_coverage_counts_are_exact_under_parallel_calls
    source = "def pick(x)\n  x.odd? ? :odd : :even\nend\ndef spin(n)\n  n -= 1 while n > 0\nend\n"
    with_source(source) do |path|
      Coverage.start(branches: true)
      load path
      threads, calls = 8, 5000
      threads.times.map { Thread.new { calls.times { |i| pick(i); spin(3) } } }.each(&:join)
      branches = Coverage.result[path][:branches]
      assert_equal({ [:then, 1, 2, 11, 2, 15] => threads * calls / 2, [:else, 2, 2, 18, 2, 23] => threads * calls / 2 }, branches[[:if, 0, 2, 2, 2, 23]])
      assert_equal({ [:body, 4, 5, 2, 5, 8] => threads * calls * 3 }, branches[[:while, 3, 5, 2, 5, 20]])
    end
  end

  def test_branch_coverage_of_blocks_turned_into_methods_and_reloaded_files
    source = "class Reloaded\n  define_method(:sign) { |x| x < 0 ? :neg : :pos }\nend\n"
    with_source(source) do |path|
      Coverage.start(branches: true)
      load path
      Reloaded.new.sign(1)
      load path                                    # loading the file again starts its counts over, as in MRI
      Reloaded.new.sign(-1)
      branches = Coverage.result[path][:branches]
      assert_equal({ [:if, 0, 2, 29, 2, 48] => { [:then, 1, 2, 37, 2, 41] => 1, [:else, 2, 2, 44, 2, 48] => 0 } }, branches)
    end
  ensure
    Object.send(:remove_const, :Reloaded) if Object.const_defined?(:Reloaded, false)
  end

  def test_branch_coverage_across_execution_modes
    with_source(BRANCHES) do |path|
      script = File.join(File.dirname(path), 'driver.rb')
      File.write(script, <<~RUBY)
        require 'coverage'
        Coverage.start(branches: true)
        load #{path.inspect}
        b = Branchy.new
        b.classify(1); b.classify(-1); b.classify(0)
        b.describe(0); b.describe(1); b.describe(200)
        b.count_down(3)
        b.safe(nil); b.safe(-2)
        b.match(20); b.match(3)
        Coverage.result[#{path.inspect}][:branches].each { |k, v| puts k.inspect; v.each { |kk, vv| puts "  \#{kk.inspect} => \#{vv}" } }
      RUBY

      expected = jruby(script).lines
      assert_equal 27, expected.size, expected.join

      ['-X-C', '-X+C', '-Xjit.threshold=0 -Xjit.background=false',
       '-Xcompile.invokedynamic=true -Xjit.threshold=0 -Xjit.background=false'].each do |flags|
        assert_equal expected, jruby("#{flags} #{script}").lines, flags
      end
    end
  end

  # Which conditionals MRI folds away (reporting no branch) and which it keeps. The expected result is CRuby's.
  def test_branch_coverage_folds_conditionals_as_mri_does
    source = <<~'RUBY'
      def folds(a)
        :a if __ENCODING__
        :b if (1; 2)
        :c if (a; [1]; {k: a}; (a..1); 2)
        :d if a || 1
        :e if (nil)
        :f if ("x")
        :g if (true && 1)
        :h if nil && 1
        :i if ([a]; 2)
        if false
          def dead(x) = x ? 1 : 2
          [1].each { |x| x ? 1 : 2 }
        end
        a => Integer
        a in String
      end
    RUBY
    with_source(source) do |path|
      Coverage.start(branches: true)
      verbose, $VERBOSE = $VERBOSE, nil # literals in conditions
      begin
        load path
      ensure
        $VERBOSE = verbose
      end
      folds(1)
      assert_equal({
        [:if, 0, 6, 2, 6, 13] => { [:then, 1, 6, 2, 6, 4] => 0, [:else, 2, 6, 2, 6, 13] => 1 },
        [:if, 3, 7, 2, 7, 13] => { [:then, 4, 7, 2, 7, 4] => 1, [:else, 5, 7, 2, 7, 13] => 0 },
        [:if, 6, 8, 2, 8, 19] => { [:then, 7, 8, 2, 8, 4] => 1, [:else, 8, 8, 2, 8, 19] => 0 },
        [:if, 9, 9, 2, 9, 16] => { [:then, 10, 9, 2, 9, 4] => 0, [:else, 11, 9, 2, 9, 16] => 1 },
        [:if, 12, 10, 2, 10, 16] => { [:then, 13, 10, 2, 10, 4] => 1, [:else, 14, 10, 2, 10, 16] => 0 },
      }, Coverage.result[path][:branches])
    end
  ensure
    Object.send(:remove_method, :folds) if Object.private_method_defined?(:folds)
  end

  def test_lone_statement_in_interpolation_keeps_earlier_statement_on_its_line
    assert_equal [1, 1, 1], line_coverage(<<~'RUBY')
      def foo(*) = nil
      foo([1].map {
        y = 1; nil }, "#{3}")
    RUBY
  end

  def test_several_statements_in_interpolation_each_cover_their_line
    assert_equal [1, 1, 1, nil], line_coverage(<<~'RUBY')
      x = "#{
        a = 1
        a
      }"
    RUBY
  end

  def test_call_on_a_later_line_keeps_the_statement_count
    assert_equal [1, 1, nil, 1, nil], line_coverage(<<~'RUBY')
      o = Struct.new(:b).new
      o.b =
        [].size
      o.b.to_s.concat(
        [].size.to_s)
    RUBY
  end

  def test_statement_counts_on_the_line_of_its_first_instruction
    assert_equal [nil, 1, nil, 1, nil, nil, nil, 1, 1], line_coverage(<<~'RUBY')
      x =
        [].size
      y = <<~EOS
        #{[].size}
        #{[].size}
      EOS
      z = {
        a: [].size }
      x = y
    RUBY
  end

  def test_literal_array_and_hash_count_on_their_first_line
    assert_equal [1, nil, nil, 1, nil, nil], line_coverage(<<~'RUBY')
      x = [
        1,
        2]
      y = {
        a: :b,
        c: 1.0}
    RUBY
  end

  def test_statement_in_begin_counts_once
    assert_equal [1, 1, nil, 1, nil, 1, nil, nil, 1], line_coverage(<<~'RUBY')
      def g
        x = 1
        begin
          [].size
        ensure
          x = 2
        end
      end
      g
    RUBY
  end

  def test_conditional_in_interpolation_counts
    assert_equal [1, nil, 1, nil, 1], line_coverage(<<~'RUBY')
      x = <<~EOS
        abc
        #{[].empty? ? "a" : "b"}
      EOS
      x = x
    RUBY
  end

  def test_heredoc_ending_the_file
    assert_equal [nil, 1, nil], line_coverage(<<~'RUBY')
      x = <<~EOS
        #{1.to_s}
      EOS
    RUBY
  end

  def test_undef_counts_on_its_keyword_line
    assert_equal [2, 1, 1, nil, nil, nil], line_coverage(<<~'RUBY')
      class UndefCoverage; def a; end; def b; end; end
      class UndefCoverage
        x = 1; undef
          a,
          b
      end
    RUBY
  end

  def test_ternary_arms_are_line_events
    assert_equal [1, 1, 1, 1, 0, 1, 1, nil], line_coverage(<<~'RUBY')
      y = [1]
      x = y.size ?
        1 : 2
      if y.empty? ?
           y.first :
           y.last
        z = 1
      end
    RUBY
  end

  # The expected results below are MRI's (4.0, prism), and a line stub reads as the counts do with every count 0.

  def test_conditional_used_as_a_value_counts_its_predicate_line
    assert_line_coverage [1, 1, 1, 1, nil, 1, 1, 0, nil, 1, 1, 1, nil, 1, 1], <<~'RUBY'
      def value_conditionals(x)
        @a ||=
          if x
            1
          end
        b = [1,
          unless x
            2
          end]
        c = value_of(
          x ?
            3 : 4)
      end
      def value_of(v) = v
      value_conditionals(true)
    RUBY
  end

  def test_elsif_conditions_are_line_events
    assert_line_coverage [1, 2, 1, 1, 1, 0, 0, nil, nil, 1, 1], <<~'RUBY'
      def elsifs(x)
        if x == 1
          :one
        elsif x == 2
          :two
        elsif x == 3
          :three
        end
      end
      elsifs(1)
      elsifs(2)
    RUBY
  end

  def test_statements_compiled_to_nothing_are_no_line_events
    assert_line_coverage [1, nil, nil, nil, nil, nil, nil, nil, 1, 1, nil, 1], <<~'RUBY'
      def void_statements(x)
        y = y
        x
        1
        "str"
        @iv
        [1, :a]
        x = x
        $stdout
        x
      end
      void_statements(1)
    RUBY
  end

  def test_conditional_on_a_literal_compiles_only_its_live_arm
    assert_line_coverage [1, nil, nil, nil, nil, 1, nil, nil, nil, nil, nil, nil, 1, nil, nil, 1, nil, nil, 1, nil, 1], <<~'RUBY'
      def literal_predicates(x)
        if false
          :dead
        end
        if true
          a = 1
        else
          b = 2
        end
        c = if nil
          3
        else
          4
        end
        d = 5 unless true
        if x and false
          6
        end
        x
      end
      literal_predicates(1)
    RUBY
  end

  def test_statement_of_a_modifier_conditional_is_no_line_event_unless_interpolated
    assert_line_coverage [1, 1, nil, 1, nil, nil, 1, 1, 1, nil, nil, 1], <<~'RUBY'
      def modified(*) = 1
      def modifiers(y)
        modified 1,
          2 if y
        modified(
          1) unless
            y
        "#{y} item#{'s' unless y == 1}"
        "#{modified 1 if
          y}"
      end
      modifiers(1)
    RUBY
  end

  # MRI compiles the arms, then the else: an else on the line of an arm is no line event of its own
  def test_case_arm_on_the_line_of_its_else_counts
    cases = <<~'RUBY'
      def one_line_cases(x)
        a = [1,
          case x; when 1 then 2; else 3; end]
        b = [1,
          case x; in 1 then 2; else 3; end]
      end
    RUBY
    assert_equal [1, 1, 1, 1, 1, nil, 1], line_coverage(cases + "one_line_cases(1)\n")
    assert_equal [1, 1, 0, 1, 0, nil, 1], line_coverage(cases + "one_line_cases(3)\n")
  end

  def test_branch_arm_of_a_call_spans_its_block
    assert_equal({
      [:if, 0, 2, 2, 2, 27] => { [:then, 1, 2, 6, 2, 23] => 1, [:else, 2, 2, 26, 2, 27] => 0 },
      [:if, 3, 3, 2, 3, 31] => { [:then, 4, 3, 6, 3, 27] => 1, [:else, 5, 3, 30, 3, 31] => 0 },
      [:if, 6, 4, 2, 4, 38] => { [:then, 7, 4, 6, 4, 34] => 1, [:else, 8, 4, 37, 4, 38] => 0 },
    }, branch_coverage(<<~'RUBY'))
      def block_arms(x)
        x ? [1].map { |v| v } : 0
        x ? [1].each do |v| v end : 0
        x ? [1].each_slice(1).map { _1 } : 0
      end
      block_arms(true)
    RUBY
  end

  def test_safe_navigation_branch_ends_before_block_arguments_and_blocks
    assert_equal({
      [:"&.", 0, 2, 2, 2, 8] => { [:then, 1, 2, 2, 2, 8] => 0, [:else, 2, 2, 2, 2, 8] => 1 },
      [:"&.", 3, 3, 2, 3, 8] => { [:then, 4, 3, 2, 3, 8] => 0, [:else, 5, 3, 2, 3, 8] => 1 },
      [:"&.", 6, 4, 2, 4, 8] => { [:then, 7, 4, 2, 4, 8] => 0, [:else, 8, 4, 2, 4, 8] => 1 },
      [:"&.", 9, 5, 2, 5, 15] => { [:then, 10, 5, 2, 5, 15] => 0, [:else, 11, 5, 2, 5, 15] => 1 },
      [:"&.", 12, 6, 2, 6, 10] => { [:then, 13, 6, 2, 6, 10] => 0, [:else, 14, 6, 2, 6, 10] => 1 },
      [:"&.", 15, 7, 2, 7, 8] => { [:then, 16, 7, 2, 7, 8] => 0, [:else, 17, 7, 2, 7, 8] => 1 },
      [:"&.", 18, 8, 2, 8, 11] => { [:then, 19, 8, 2, 8, 11] => 0, [:else, 20, 8, 2, 8, 11] => 1 },
    }, branch_coverage(<<~'RUBY'))
      def safe_navigation(o, b)
        o&.foo
        o&.foo()
        o&.foo(&b)
        o&.foo(1, &b)
        o&.foo 1, &b
        o&.foo { 1 }
        o&.foo(1) do 1 end
      end
      safe_navigation(nil, nil)
    RUBY
  end

  def test_branch_arms_of_literals_span_what_mri_reports
    assert_equal({
      [:if, 0, 2, 2, 2, 18] => { [:then, 1, 2, 6, 2, 14] => 1, [:else, 2, 2, 17, 2, 18] => 0 },
      [:if, 3, 3, 2, 3, 13] => { [:then, 4, 3, 6, 3, 8] => 1, [:else, 5, 3, 11, 3, 13] => 0 },
      [:if, 6, 4, 6, 4, 18] => { [:then, 7, 4, 10, 4, 14] => 1, [:else, 8, 4, 17, 4, 18] => 0 },
      [:if, 9, 7, 2, 9, 5] => { [:then, 10, 8, 4, 8, 11] => 1, [:else, 11, 7, 2, 9, 5] => 0 },
    }, branch_coverage(<<~'RUBY'))
      def literal_arms(x)
        x ? -> { 1 } : 2
        x ? ?a : ?b
        y = x ? <<~A : 2
          a
        A
        if x
          END { }
        end
      end
      literal_arms(true)
    RUBY
  end

  def test_line_stub_has_an_entry_for_every_line
    source = <<~'RUBY'
      def show
        @product = base_scope
                   .includes(colors_products: :color)
                   .find(params[:id])
      end
    RUBY
    with_source(source) { |path| assert_equal [0, 0, nil, nil, nil], Coverage.line_stub(path) }
    with_source(source.chomp) { |path| assert_equal [0, 0, nil, nil, nil], Coverage.line_stub(path) }
    with_source("x = 1\n\n\n\n") { |path| assert_equal [0, nil, nil, nil], Coverage.line_stub(path) }
    with_source("") { |path| assert_equal [], Coverage.line_stub(path) }
  end

  def test_line_stub_of_an_empty_file_after_an_empty_else
    source = <<~'RUBY'
      x.each do
        if x
          1
        else
        end
      end
    RUBY
    with_source(source) { |path| Coverage.line_stub(path) }
    with_source("") { |path| assert_equal [], Coverage.line_stub(path) }
  end

  private

  def with_source(source)
    @source = source
    Dir.mktmpdir do |dir|
      path = File.join(File.realpath(dir), 'covered.rb') # JRuby reports loaded files by their real path
      File.write(path, source)
      yield path
    end
  ensure
    @source = nil
  end

  # The key Coverage reports for a one-line definition: [owner, name, line, start_column, line, end_column].
  # The columns are found in the source text of that line.
  def key(owner, name, line, from, to)
    text = source_line(line)
    [owner, name, line, text.index(from), line, text.rindex(to) + to.length]
  end

  # "line start_column line end_column" for a one-line definition, as printed by the driver script above
  def span(line, from, to)
    key(nil, nil, line, from, to).drop(2).join(' ')
  end

  def source_line(line)
    @source.lines[line - 1]
  end

  def line_coverage(code)
    with_source(code) do |path|
      Coverage.start(lines: true)
      load path
      Coverage.result.fetch(path)[:lines]
    end
  end

  # The line counts, and the line stub as the same lines with every count 0
  def assert_line_coverage(expected, code)
    assert_equal expected, line_coverage(code)
    with_source(code) { |path| assert_equal expected.map { |count| count && 0 }, Coverage.line_stub(path) }
  end

  def branch_coverage(code)
    with_source(code) do |path|
      Coverage.start(branches: true)
      verbose, $VERBOSE = $VERBOSE, nil # END in a method warns
      load path
      Coverage.result.fetch(path)[:branches]
    ensure
      $VERBOSE = verbose
    end
  end
end
