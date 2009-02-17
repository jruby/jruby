###########################################################
# tc_aset.rb
#
# Test suite for the Hash[] class method.
###########################################################
require "test/unit"

class TC_Hash_Aset_ClassMethod < Test::Unit::TestCase
   def test_empty
      assert_equal({}, Hash[])
   end

   def test_aset_strings
      assert_equal({"foo"=>1}, Hash["foo"=>1])
      assert_equal({"foo"=>1}, Hash["foo",1])
      assert_equal({"foo"=>1, "bar"=>2}, Hash["foo"=>1, "bar"=>2])
   end

   def test_aset_symbols
      assert_equal({:foo=>1}, Hash[:foo=>1])
      assert_equal({:foo=>1}, Hash[:foo,1])
      assert_equal({:foo=>1, :bar=>2}, Hash[:foo=>1, :bar=>2])
   end

   def test_aset_expected_errors
      assert_raises(ArgumentError){ Hash["foo"] } # Odd number of arguments
   end
end
