###############################################################################
# tc_id2name.rb
#
# Test case for the Symbol#id2name instance method.
###############################################################################
require 'test/unit'

class TC_Symbol_Id2name_InstanceMethod < Test::Unit::TestCase
   def setup
      @symbol = :hello
      @string = 'world'
   end

   def test_id2name_basic
      assert_respond_to(@symbol, :id2name)
      assert_nothing_raised{ @symbol.id2name }
      assert_kind_of(String, @symbol.id2name)
   end

   def test_id2name
      assert_equal('hello', @symbol.id2name)
      assert_equal('99', :"99".id2name)
      assert_equal('[1,2,3]', :"[1,2,3]".id2name)
   end

   def test_id2name_edge_cases
      assert_equal('@string', :@string.id2name)
   end

   def test_id2name_expected_errors
      assert_raise(ArgumentError){ @symbol.id2name('test') }
   end

   def teardown
      @symbol = nil
      @string = nil
   end
end
