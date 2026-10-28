require 'test/unit'
require 'test/jruby/test_helper'

# Core fast paths skip a dynamic call while the method they stand in for is
# still the builtin one, as tracked by org.jruby.runtime.Builtins. Each case
# runs in a separate runtime so the redefinition does not leak into other tests.
class TestBuiltins < Test::Unit::TestCase
  include TestHelper

  FAST = <<~RUBY
    require 'jruby'
    def fast?(bop, klass)
      builtins = org.jruby.runtime.Builtins
      (JRuby.runtime.builtin_bits[builtins.const_get(bop)] & builtins.const_get(klass)).equal?(0)
    end
  RUBY

  def test_no_fast_path_is_disabled_by_boot
    assert_equal '[]', run_in_sub_runtime(<<~RUBY)
      require 'jruby'
      JRuby.runtime.builtin_bits.to_a.each_with_index.reject { |bits, _| bits.equal?(0) }.inspect
    RUBY
  end

  def test_changes_that_disable_a_fast_path
    {
      'class Integer; def ==(other) = true; end' => %i[BOP_EQ INTEGER],
      'class Integer; def eql?(other) = true; end' => %i[BOP_EQ INTEGER],
      'class Integer; def <=>(other) = 0; end' => %i[BOP_CMP INTEGER],
      'class Integer; undef_method :<; end' => %i[BOP_LT INTEGER],
      'class Integer; remove_method :+; end' => %i[BOP_PLUS INTEGER],
      'class Integer; alias_method :+, :-; end' => %i[BOP_PLUS INTEGER],
      'class String; def hash = 0; end' => %i[BOP_HASH STRING],
      'class String; def <=>(other) = 0; end' => %i[BOP_CMP STRING],
      'class Hash; def default(key = nil) = 0; end' => %i[BOP_DEFAULT HASH],
      'class Time; def <=>(other) = 0; end' => %i[BOP_CMP TIME],
      # inherited methods redefined in an ancestor
      'class Numeric; def eql?(other) = true; end' => %i[BOP_EQ INTEGER],
      'module Kernel; def nil? = true; end' => %i[BOP_NIL_P INTEGER],
      'class Object; def !=(other) = true; end' => %i[BOP_NEQ STRING],
      'class BasicObject; def !; true; end; end' => %i[BOP_NOT STRING],
      # modules prepended or included before or after their methods are defined
      'Integer.prepend(Module.new { def ==(other) = true })' => %i[BOP_EQ INTEGER],
      'Float.prepend(Module.new { def +(other) = 0 })' => %i[BOP_PLUS FLOAT],
      'M = Module.new; Integer.prepend(M); module M; def <=>(other) = 0; end' => %i[BOP_CMP INTEGER],
      'Integer.include(Module.new { def eql?(other) = true })' => %i[BOP_EQ INTEGER],
      'M = Module.new; Integer.include(M); module M; def eql?(other) = true; end' => %i[BOP_EQ INTEGER],
      'M = Module.new; Numeric.prepend(M); module M; def eql?(other) = true; end' => %i[BOP_EQ INTEGER],
      'N = Module.new; M = Module.new { include N }; Integer.prepend(M); module N; def ==(other) = true; end' => %i[BOP_EQ INTEGER],
      'N = Module.new { def ==(other) = true }; M = Module.new { include N }; Integer.prepend(M)' => %i[BOP_EQ INTEGER],
      # refinements apply only where they are active, but the fast paths cannot tell
      'module Fast; refine(Integer) { def ==(other) = true }; end' => %i[BOP_EQ INTEGER]
    }.each do |change, (bop, klass)|
      result = run_in_sub_runtime(<<~RUBY)
        #{FAST}
        before = fast?(:#{bop}, :#{klass})
        #{change}
        [before, fast?(:#{bop}, :#{klass})].inspect
      RUBY
      assert_equal '[true, false]', result, change
    end
  end

  def test_changes_that_keep_fast_paths
    fast_paths = %i[
      BOP_EQ:INTEGER BOP_CMP:INTEGER BOP_PLUS:INTEGER BOP_LT:INTEGER BOP_EQ:FLOAT BOP_PLUS:FLOAT
      BOP_HASH:STRING BOP_CMP:STRING BOP_DEFAULT:HASH BOP_CMP:TIME
    ]
    [
      'class Foo; def ==(other) = true; def hash = 0; def <=>(other) = 0; def default = 0; end',
      'class MyString < String; def hash = 0; def <=>(other) = 0; end',
      'class MyHash < Hash; def default(key = nil) = 0; end',
      's = +""; def s.hash = 0',
      'Module.new { def ==(other) = true; def hash = 0; def <=>(other) = 0 }',
      # Integer, Float, String and Time define these themselves, ahead of the ancestors
      'class Numeric; def ==(other) = true; def <=>(other) = 0; def +(other) = 0; end',
      'module Comparable; def ==(other) = true; def <(other) = true; end',
      'module Comparable; def eql?(other) = true; end',
      'module Kernel; def hash = 0; def eql?(other) = true; end',
      'class Object; def <=>(other) = 0; def eql?(other) = true; end',
      'String.include(Module.new { def hash = 0; def <=>(other) = 0 })',
      'M = Module.new; Numeric.include(M); module M; def eql?(other) = true; end',
      'Integer.prepend(Module.new { def foo = 1 })',
      'class Integer; def foo = 1; end',
      'class Integer; alias_method :old_plus, :+; end',
      'class Integer; private :+; public :+; end'
    ].each do |change|
      result = run_in_sub_runtime(<<~RUBY)
        #{FAST}
        #{change}
        #{fast_paths.map { _1.to_s.split(':') }.inspect}.reject { |bop, klass| fast?(bop, klass) }.inspect
      RUBY
      assert_equal '[]', result, change
    end
  end

  def test_array_search_and_equality_honor_prepended_equality
    {
      'Integer.prepend(Module.new { def ==(other) = true })' => ['[2**40, 2**40 + 1]', '[true, 0, 1, true, true]'],
      'Float.prepend(Module.new { def ==(other) = true })' => ['[1.5, 2.5]', '[true, 0, 1, true, false]']
    }.each do |redefinition, (values, expected)|
      result = run_in_sub_runtime(<<~RUBY)
        a, b = #{values}
        [[a].include?(b), [a].index(b), [a].count(b), [a] == [b], [a, a].eql?([b, b])]
        #{redefinition}
        [[a].include?(b), [a].index(b), [a].count(b), [a] == [b], [a, a].eql?([b, b])].inspect
      RUBY
      assert_equal expected, result, redefinition
    end
  end

  def test_sort_honors_redefined_comparison
    [
      'Integer.prepend(Module.new { def <=>(other) = -super })',
      'M = Module.new; Integer.prepend(M); module M; def <=>(other) = -super; end',
      'class Integer; def <=>(other) = other > self ? 1 : other < self ? -1 : 0; end'
    ].each do |redefinition|
      result = run_in_sub_runtime(<<~RUBY)
        [2, 3, 1].sort
        #{redefinition}
        [[2, 3, 1].sort, [1, 2].sort].inspect
      RUBY
      assert_equal '[[3, 2, 1], [2, 1]]', result, redefinition
    end
  end

  def test_sort_honors_string_subclass_and_singleton_comparison
    reversed = Class.new(String) { def <=>(other) = -super }
    assert_equal %w[b a], [reversed.new('a'), reversed.new('b')].sort
    assert_equal %w[c b a], [reversed.new('b'), reversed.new('c'), reversed.new('a')].sort

    a, b = +'a', +'b'
    def a.<=>(other) = 1
    def b.<=>(other) = -1
    assert_equal %w[b a], [a, b].sort
    assert_equal %w[b a], [b, a].sort
  end
end
