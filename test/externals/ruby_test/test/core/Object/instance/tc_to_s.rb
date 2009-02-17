########################################################################
# tc_to_s.rb
#
# Test case for the Object#to_s instance method.
########################################################################
require 'test/unit'

$object_to_s_main = to_s # Special case

class TC_Object_ToS_InstanceMethod < Test::Unit::TestCase
   def setup
      @object = Object.new
      @cname  = Object.class.to_s
   end

   def test_to_s_basic
      assert_respond_to(@object, :to_s)
      assert_nothing_raised{ @object.to_s }
      assert_kind_of(String, @object.to_s)
   end

   def test_to_s
      assert_not_nil(@object.to_s =~ /^#<\w+:0x\w+>$/)
      assert_equal('main', $object_to_s_main)
   end

   def test_to_s_tainted
      assert_nothing_raised{ @object.taint }
      assert_equal(true, @object.to_s.tainted?)
   end

   def test_to_s_expected_errors
      assert_raise(ArgumentError){ @object.to_s(1) }
   end

   def teardown
      @object = nil
      @cname  = nil
   end
end
