###############################################################################
# tc_all_symbols.rb
#
# Test case for the Symbol.all_symbols class method.
###############################################################################
require 'test/unit'

class TC_Symbol_AllSymbols_ClassMethod < Test::Unit::TestCase
   def test_all_symbols_basic
      assert_respond_to(Symbol, :all_symbols)
      assert_nothing_raised{ Symbol.all_symbols }
      assert_kind_of(Array, Symbol.all_symbols)
   end

   def test_all_symbols
      assert_equal(true, Symbol.all_symbols.first.is_a?(Symbol))
      assert_equal(true, Symbol.all_symbols.include?(:upcase))
   end
end
