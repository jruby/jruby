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

  # The Ruby specs (spec/ruby/library/coverage) cover method coverage as every Ruby reports it. These tests cover
  # what is specific to JRuby: the execution engines, real parallelism, and the ways a method entry can be created.

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

  def test_method_coverage_keys_and_counts_of_blocks_turned_into_methods
    with_source(METHODS) do |path|
      Coverage.start(methods: true)
      load path
      c = Covered.new
      c.block(1)
      2.times { c.lam(1) }
      methods = Coverage.result[path][:methods]
      assert_equal 12, methods.size
      assert_equal 1, methods[key(Covered, :block, 3, '{', '}')]
      assert_equal 2, methods[key(Covered, :lam, 4, '(x)', '}')]
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

  def test_method_coverage_counts_only_block_calls_that_get_past_argument_processing
    with_source(METHODS) do |path|
      Coverage.start(methods: true)
      load path
      c = Covered.new
      assert_raise(ArgumentError) { c.block(1, 2) }
      assert_raise(ArgumentError) { c.lam }
      methods = Coverage.peek_result[path][:methods]
      assert_equal 0, methods[key(Covered, :block, 3, '{', '}')]
      assert_equal 0, methods[key(Covered, :lam, 4, '(x)', '}')]

      c.block(1)
      c.lam(1)
      methods = Coverage.result[path][:methods]
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

  # Branch coverage. The Ruby specs cover what a branch is and where it is; these tests cover what is specific to
  # JRuby, as for method coverage above.

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
end
