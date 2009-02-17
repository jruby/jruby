########################################################################
# tc_equality.rb
#
# Test case for the Method#== instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class MethodEquality
   def foo; end
   def bar; some_method; end  # Synonym, but not true alias
   alias baz foo
end

class TC_Method_Equality_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @meth  = MethodEquality.instance_method(:foo)
      @syn   = MethodEquality.instance_method(:bar)
      @alias = MethodEquality.instance_method(:baz)
   end

   def test_equality_basic
      assert_respond_to(@meth, :==)
      assert_nothing_raised{ @meth == @syn }
      assert_kind_of(Boolean, @meth == @alias)
   end

   def test_equality
      assert_equal(true, @meth == @meth)
      assert_equal(false, @meth == @syn)
      assert_equal(true, @meth == @alias)
      assert_equal(false, @meth == 'foo')
      assert_equal(false, @meth == 'foo'.to_sym)
   end

   def test_equality_expected_errors
      assert_raise(ArgumentError){ @meth.send(:==) }
      assert_raise(ArgumentError){ @meth.send(:==, @meth, @syn) }
   end

   def teardown
      @meth  = nil
      @syn   = nil
      @alias = nil
   end
end
