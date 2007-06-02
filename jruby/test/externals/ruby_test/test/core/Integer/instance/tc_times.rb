#####################################################################
# tc_times.rb
#
# Test case for the Integer#times method.
#####################################################################
require 'test/unit'

class TC_Integer_Times_Instance < Test::Unit::TestCase
   def setup
      @int1 = 65
      @temp = []
   end

   def test_times_basic
      assert_respond_to(@int1, :times)
   end

   def test_times
      assert_nothing_raised{ @int1.times{} }
      assert_nothing_raised{ 100.times{} }
      assert_nothing_raised{ -1.times{} }

      assert_nothing_raised{ @int1.times{ |i| @temp << i } }
      assert_equal(65, @temp.size)
   end

   def test_times_expected_errors
      assert_raises(ArgumentError){ @int1.times(2) }
   end

   def teardown
      @int1 = nil
      @temp = nil
   end
end
