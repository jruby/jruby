#####################################################################
# tc_upto.rb
#
# Test case for the Integer#upto method.
#####################################################################
require 'test/unit'

class TC_Integer_Upto_Instance < Test::Unit::TestCase
   def setup
      @int1 = 65
      @temp = []
   end

   def test_upto_basic
      assert_respond_to(@int1, :upto)
   end

   def test_upto
      assert_nothing_raised{ @int1.upto(0){} }
      assert_nothing_raised{ @int1.upto(100){} }
      assert_nothing_raised{ @int1.upto(-100){} }

      assert_nothing_raised{ 0.upto(@int1){ |i| @temp << i } }
      assert_equal(66, @temp.size)
   end

   def teardown
      @int1 = nil
      @temp = nil
   end
end
