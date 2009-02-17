########################################################################
# tc_equality.rb
#
# Test case for the Method#.eql?( instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class MethodEquality
   def foo; end
   def bar; some_method; end  # Synonym, but not true alias
   alias baz foo
end

class TC_Method_Eql_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @meth  = MethodEquality.instance_method(:foo)
      @syn   = MethodEquality.instance_method(:bar)
      @alias = MethodEquality.instance_method(:baz)
   end

   def test_equality_basic
      assert_respond_to(@meth, :eql?)
      assert_nothing_raised{ @meth.eql?(@syn) }
      assert_kind_of(Boolean, @meth.eql?( @alias))
   end

   def test_equality
      assert_equal(true, @meth.eql?(@meth))
      assert_equal(false, @meth.eql?(@syn))
      assert_equal(false, @meth.eql?(@alias))
      assert_equal(false, @meth.eql?('foo'))
      assert_equal(false, @meth.eql?('foo'.to_sym))
   end

   def test_equality_expected_errors
      assert_raise(ArgumentError){ @meth.eql?() }
      assert_raise(ArgumentError){ @meth.eql?(@meth, @syn) }
   end

   def teardown
      @meth  = nil
      @syn   = nil
      @alias = nil
   end
end
