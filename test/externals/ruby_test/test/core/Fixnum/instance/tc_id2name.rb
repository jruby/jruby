###############################################################################
# tc_id2name.rb
#
# Test case for the Fixnum#id2name instance method.
###############################################################################
require 'test/unit'

class TC_Fixnum_Id2name_InstanceMethod < Test::Unit::TestCase
   def setup
      @foo = "hello"
      @sym = :@foo
      @num = @sym.to_i
   end

   def test_id2name_basic
      assert_respond_to(@sym, :id2name)
      assert_nothing_raised{ @sym.id2name }
   end

   def test_id2name
      assert_equal("@foo", @num.id2name)
      assert_equal(nil, 999999999.id2name)
   end

   def test_id2name_expected_errors
      assert_raise(ArgumentError){ @num.id2name(1) }
   end

   def teardown
      @foo = nil
      @sym = nil
      @num = nil
   end
end
