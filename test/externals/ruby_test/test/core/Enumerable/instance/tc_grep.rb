#########################################################################
# tc_grep.rb
#
# Test suite for the Enumerable#grep instance method.
#########################################################################
require 'test/unit'

class MyEnumGrep
   include Enumerable

   attr_accessor :arg1, :arg2, :arg3

   def initialize(arg1, arg2, arg3)
      @arg1 = arg1
      @arg2 = arg2
      @arg3 = arg3
   end

   def each
      yield @arg1
      yield @arg2
      yield @arg3
   end
end

class TC_Enumerable_Grep_InstanceMethod < Test::Unit::TestCase
   def setup
      @enum = MyEnumGrep.new('alpha', 7, 'beta')
   end

   def test_grep_basic
      assert_respond_to(@enum, :grep)
      assert_nothing_raised{ @enum.grep(/a/) }
      assert_nothing_raised{ @enum.grep(/a/){} }
   end

   def test_grep
      assert_equal([7], @enum.grep(7))
      assert_equal(['alpha', 'beta'], @enum.grep(/a/))
      assert_equal(['beta'], @enum.grep(/ta/))
   end

   def test_grep_with_block
      array = []
      assert_nothing_raised{ @enum.grep(/a/){ |m| array << (m == 'alpha') } }
      assert_equal([true, false], array)
   end

   # Test some other kinds of patterns
   def test_grep_threequals
      assert_equal([38, 39, 40], (1..100).grep(38..40))
   end

   def test_grep_explicit_false_and_nil
      @enum = MyEnumGrep.new(nil, false, nil)
      assert_equal([], @enum.grep(true))
      assert_equal([false], @enum.grep(false))
      assert_equal([nil, nil], @enum.grep(nil))
   end

   def test_grep_edge_cases
      assert_equal([], ['alpha', 'beta', 'gamma'].grep('a'))
      assert_equal([], ['alpha', 'beta', 'gamma'].grep('ALPHA'))
   end

   def test_grep_expected_errors
      assert_raise(ArgumentError){ @enum.grep }
   end

   def teardown
      @enum   = nil
   end
end
