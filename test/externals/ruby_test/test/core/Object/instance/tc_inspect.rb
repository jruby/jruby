########################################################################
# tc_inspect.rb
#
# Test case for the Object#inspect instance method.
########################################################################
require 'test/unit'

$object_inspect_main = inspect # Special case

class TC_Object_Inspect_InstanceMethod < Test::Unit::TestCase
   def setup
      @object = Object.new
      @cname  = Object.class.inspect
   end

   def test_inspect_basic
      assert_respond_to(@object, :inspect)
      assert_nothing_raised{ @object.inspect }
      assert_kind_of(String, @object.inspect)
   end

   def test_inspect
      assert_not_nil(@object.inspect =~ /^#<\w+:0x\w+>$/)
      assert_equal('main', $object_inspect_main)
   end

   def test_inspect_tainted
      assert_nothing_raised{ @object.taint }
      assert_equal(true, @object.inspect.tainted?)
   end

   def test_inspect_expected_errors
      assert_raise(ArgumentError){ @object.inspect(1) }
   end

   def teardown
      @object = nil
      @cname  = nil
   end
end
