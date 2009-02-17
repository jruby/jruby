################################################################
# tc_new.rb
#
# Test suite for the Hash.new class method.
################################################################
require "test/unit"

class TC_Hash_New_ClassMethod < Test::Unit::TestCase
   def setup
      @hash = nil
   end

   def test_basic
      assert_respond_to(Hash, :new)
   end

   def test_new
      assert_nothing_raised{ Hash.new }
      assert_kind_of(Hash, Hash.new)
   end

   def test_new_default_value
      assert_nothing_raised{ Hash.new("test") }
      assert_nothing_raised{ Hash.new(0) }
      assert_nothing_raised{ Hash.new(nil) }
      assert_nothing_raised{ Hash.new(false) }
   end

   def test_new_default_value_behavior
      assert_nothing_raised{ @hash = Hash.new("test") }
      assert_equal("test", @hash["foo"])
      assert_equal("test", @hash["bar"])
   end

   def test_new_with_block
      assert_nothing_raised{ Hash.new{ } }
      assert_nothing_raised{ Hash.new{ |hash, key| } }
   end

   def test_new_with_block_behavior
      assert_nothing_raised{ @hash = Hash.new{ |h,k| h[k] = "test" } }
      assert_equal("test", @hash["foo"])
      assert_equal("test", @hash["bar"])
   end

   def test_expected_errors
      assert_raise(ArgumentError){ Hash.new(1,2) }  # too many arguments
      assert_raise(ArgumentError){ Hash.new(0){ } } # arg + block illegal
   end

   def teardown
      @hash = nil
   end
end
