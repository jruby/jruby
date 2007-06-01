###########################################################
# tc_default_proc.rb
#
# Test suite for the Hash#default_proc instance method.
###########################################################
require "test/unit"

class TC_Hash_DefaultProc_Instance < Test::Unit::TestCase
   def setup
      @hash1 = Hash.new{ |h,k| h[k] = k*k }
      @hash2 = Hash.new
   end

   def test_default_proc_basic
      assert_respond_to(@hash1, :default_proc)
      assert_nothing_raised{ @hash1.default_proc }
   end

   def test_default_proc
      assert_kind_of(Proc, @hash1.default_proc)
      assert_equal(nil, @hash2.default_proc)
      assert_nothing_raised{ @hash1.default_proc.call([],2) }
   end

   def test_default_proc_expected_errors
      assert_raises(ArgumentError){ @hash1.default_proc(1) }
   end

   def teardown
      @hash1 = nil
      @hash2 = nil
   end
end
