###############################################################################
# tc_inspect.rb
#
# Test case for the Symbol#inspect instance method.
###############################################################################
require 'test/unit'

class TC_Symbol_Inspect_InstanceMethod < Test::Unit::TestCase
   def setup
      @symbol = :hello
      @colons = "::world".to_sym
   end

   def test_inspect_basic
      assert_respond_to(@symbol, :inspect)
      assert_nothing_raised{ @symbol.inspect }
      assert_kind_of(String, @symbol.inspect)
   end

   def test_inspect
      assert_equal(':hello', @symbol.inspect)
      assert_equal(':"::world"', @colons.inspect)
   end

   def test_inspect_expected_errors
      assert_raise(ArgumentError){ @symbol.inspect(1) }
   end

   def teardown
      @symbol = nil
      @colons = nil
   end
end
