###########################################################
# tc_aref.rb
#
# Test suite for the Hash#[] instance method.
###########################################################
require "test/unit"

class TC_Hash_Aref_Instance < Test::Unit::TestCase
   def setup
      @hash = {:foo=>1, "bar"=>2, nil=>3, false=>4, "foo"=>5}
   end

   def test_aref_basic
      assert_respond_to(@hash, :[])
      assert_nothing_raised{ @hash["bar"] }
   end

   def test_aref
      assert_equal(1, @hash[:foo])
      assert_equal(2, @hash["bar"])
      assert_equal(3, @hash[nil])
      assert_equal(4, @hash[false])
      assert_equal(5, @hash["foo"])
      assert_equal(nil, @hash["bogus"])
   end

   def test_aref_expected_errors
      assert_raises(ArgumentError){ @hash[] }
      assert_raises(ArgumentError){ @hash[1,2] }
   end

   def teardown
      @hash = nil
   end
end
