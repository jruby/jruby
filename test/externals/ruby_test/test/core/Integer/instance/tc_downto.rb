#####################################################################
# tc_downto.rb
#
# Test case for the Integer#downto method.
#####################################################################
require 'test/unit'

class TC_Integer_Downto_Instance < Test::Unit::TestCase
   def setup
      @int1 = 65
      @temp = []
   end

   def test_downto_basic
      assert_respond_to(@int1, :downto)
   end

   def test_downto
      assert_nothing_raised{ @int1.downto(0){} }
      assert_nothing_raised{ @int1.downto(100){} }
      assert_nothing_raised{ @int1.downto(-100){} }

      assert_nothing_raised{ @int1.downto(0){ |i| @temp << i } }
      assert_equal(66, @temp.size)
   end

   def teardown
      @int1 = nil
      @temp = nil
   end
end
