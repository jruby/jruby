require 'test/unit'
require 'coverage'
require 'tmpdir'
require_relative 'test_helper'

class TestCoverage < Test::Unit::TestCase
  include TestHelper

  def teardown
    Coverage.result if Coverage.state != :idle
    # each test loads METHODS again; drop the classes so definitions do not pile up across tests
    [:Sub, :Prepended, :Mixin, :Covered].each { |c| Object.send(:remove_const, c) if Object.const_defined?(c, false) }
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

  # define_method(name, method_object) binds a copy that keeps its own name. MRI keys the entry by the name it
  # was defined under, not the name it was copied from.
  def test_method_coverage_keys_define_method_from_method_object_by_its_new_name
    source = "class Covered\n  def original; end\nend\nclass Sub < Covered\n  define_method(:renamed, instance_method(:original))\nend\n"
    with_source(source) do |path|
      Coverage.start(methods: true)
      load path
      Sub.new.renamed
      methods = Coverage.result[path][:methods]
      assert_equal 1, methods[key(Sub, :renamed, 2, 'def', 'end')]
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
end
