########################################################################
# tc_new.rb
#
# Test case for the Time.new class method.
########################################################################
require 'test/unit'

class TC_Time_New_ClassMethod < Test::Unit::TestCase
   def test_new
      assert_respond_to(Time, :new)
      assert_nothing_raised{ Time.new }
      assert_kind_of(Time, Time.new)
   end

   def test_new_expected_errors
      assert_raise(ArgumentError){ Time.new(0) }
   end
end
