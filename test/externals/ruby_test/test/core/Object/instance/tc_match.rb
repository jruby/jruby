########################################################################
# tc_match.rb
#
# Test case for the Object#=~ instance method. This is a very short
# test case because Object#=~ always returns false. It is meant to be
# overridden by subclasses.
########################################################################
require 'test/unit'

class TC_Object_Match_InstanceMethod < Test::Unit::TestCase
   def setup
      @object = Object.new
   end

   def test_match_basic
      assert_respond_to(@object, :=~)
      assert_nothing_raised{ @object =~ @object }
   end

   def test_match
      assert_equal(false, @object =~ @object)
   end

   def test_match_expected_failures
      assert_raise(ArgumentError){ @object.send(:=~) }
      assert_raise(ArgumentError){ @object.send(:=~, 1, 2) }
   end

   def teardown
      @object = nil
   end
end
