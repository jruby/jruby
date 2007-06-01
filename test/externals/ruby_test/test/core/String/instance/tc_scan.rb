######################################################################
# tc_scan.rb
#
# Test case for the String#scan instance method.
# 
# TODO: This could use some more thorough tests.
######################################################################
require 'test/unit'

class TC_String_Scan_Instance < Test::Unit::TestCase
   def setup
      @string = "cruel world"
      @empty  = ""
   end

   def test_scan_basic
      assert_respond_to(@string, :scan)
      assert_nothing_raised{ @string.scan(//) }
      assert_nothing_raised{ @string.scan(//){} }
   end

   def test_scan
      assert_equal(['cruel', 'world'], @string.scan(/\w+/))
      assert_equal(['cru', 'el ', 'wor'], @string.scan(/.../))
      assert_equal([['cru'], ['el '], ['wor']], @string.scan(/(...)/))
      assert_equal([['cr', 'ue'], ['l ', 'wo']], @string.scan(/(..)(..)/))
   end

   def test_scan_block
      array = []
      assert_nothing_raised{ @string.scan(/\w+/){ |word| array << word } }
      assert_equal(['cruel', 'world'], array)
   end

   def test_scan_edge_cases
      assert_equal([], @empty.scan(/\w+/))
      assert_equal([''], @empty.scan(//)) # ???
   end

   def teardown
      @string = nil
   end
end
