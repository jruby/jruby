###################################################################
# tc_nil.rb
#
# Test suite for the NilClass#nil? instance method.
###################################################################
require "test/unit"

class TC_NilClass_Nil_Instance < Test::Unit::TestCase
   def setup
      @nil   = nil
      @zero  = 0
      @false = false
      @proc  = Proc.new{ nil }
      @empty = ""
   end

   def test_basic
      assert_respond_to(@nil, :nil?)
      assert_respond_to(@zero, :nil?)
      assert_respond_to(@false, :nil?)
      assert_respond_to(@proc, :nil?)

      assert_nothing_raised{ @nil.nil? }
      assert_nothing_raised{ @zero.nil? }
      assert_nothing_raised{ @false.nil? }
      assert_nothing_raised{ @proc.nil? }
   end
   
   def test_nil
      assert_equal(true, @nil.nil?)
      assert_equal(false, @zero.nil?)
      assert_equal(false, @false.nil?)
      assert_equal(false, @proc.nil?)
      assert_equal(false, @empty.nil?)
   end

   def teardown
      @nil   = nil
      @zero  = nil
      @false = nil
      @proc  = nil
      @empty = nil
   end
end
