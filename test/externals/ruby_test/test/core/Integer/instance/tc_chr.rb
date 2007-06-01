#####################################################################
# tc_chr.rb
#
# Test case for the Integer#chr method.
#####################################################################
require 'test/unit'

class TC_Integer_Chr_Instance < Test::Unit::TestCase
   def setup
      @int1 = 65
   end

   def test_chr_basic
      assert_respond_to(@int1, :chr)
   end

   def test_chr
      assert_nothing_raised{ @int1.chr }
      assert_equal('A', @int1.chr)
      assert_equal('A', 65.chr)
   end

   def test_chr_expected_errors
      assert_raises(ArgumentError){ @int1.chr(2) }
      assert_raises(RangeError){ -1.chr }
   end

   def teardown
      @int1 = nil
   end
end
