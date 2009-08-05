####################################################################
# tc_reject.rb
#
# Test suite for the Hash#reject and Hash#reject! instance methods.
####################################################################
require "test/unit"

class TC_Hash_Reject_Instance < Test::Unit::TestCase
   def setup
      @hash = {:foo, 1, "bar", 2, nil, 3, false, 4}
   end

   def test_reject_basic
      assert_respond_to(@hash, :reject)
      assert_nothing_raised{ @hash.reject{} }
      assert_nothing_raised{ @hash.reject!{} }
   end

   def test_reject
      assert_equal({:foo, 1, "bar", 2}, @hash.reject{ |k,v| v > 2 })
      assert_equal(
         {:foo, 1, "bar", 2, nil, 3, false, 4},
         @hash.reject{ |k,v| v > 5 }
      )
      assert_equal({}, @hash.reject{ |k,v| v >= 0 })
   end

   def test_reject_bang
      assert_equal({:foo, 1, "bar", 2}, @hash.reject!{ |k,v| v > 2 })
      assert_equal({:foo, 1, "bar", 2}, @hash)
      assert_nil(@hash.reject!{ |k,v| v > 99 })
   end

   def test_reject_expected_errors
   # No longer a valid test in 1.8.7
=begin
      assert_raises(LocalJumpError){ @hash.reject }
=end      assert_raises(ArgumentError){ @hash.reject(1){} }
   end

   def teardown
      @hash = nil
   end
end
