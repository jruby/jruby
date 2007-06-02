###########################################################
# tc_rindex.rb
#
# Test suite for the String#rindex instance method.
###########################################################
require "test/unit"

class TC_String_RIndex_Instance < Test::Unit::TestCase
   def setup
      @simple  = "hello"
      @complex = "p1031'/> <b><c n='field'/><c n='fl"
   end

   def test_rindex_basic
      assert_respond_to(@simple, :rindex)
      assert_nothing_raised{ @simple.rindex(0) }
      assert_nothing_raised{ @simple.rindex("l") }
      assert_nothing_raised{ @simple.rindex(/\w+/) }
   end

   def test_rindex_string_complex
      assert_equal(27, @complex.rindex("c"))
      assert_equal(9, @complex.rindex("<b><c n='field'/><c n='fl"))
   end

   def test_rindex_string
      assert_equal(3, @simple.rindex("l"))
      assert_equal(1, @simple.rindex("ell"))
      assert_nil(@simple.rindex("z"))
   end

   def test_rindex_string_with_position
      assert_equal(2, @simple.rindex("l",2))
      assert_nil(@simple.rindex("l",1))
      assert_nil(@simple.rindex("z",1))
      assert_nil(@simple.rindex("z",99))
   end

   def test_rindex_int
      assert_equal(3, @simple.rindex(?l))
      assert_nil(@simple.rindex(?z))
   end

   def test_rindex_int_with_position
      assert_equal(0, @simple.rindex(?h,0))
      assert_equal(3, @simple.rindex(?l,3))
      assert_nil(@simple.rindex(?l,1))
      assert_nil(@simple.rindex(?e,0))
      assert_nil(@simple.rindex(?z,0))
   end

   def test_rindex_regex
      assert_equal(0, @simple.rindex(/h/))
      assert_equal(2, @simple.rindex(/ll./))
      assert_nil(@simple.rindex(/z./))
   end

   def test_rindex_regex_with_offset
      assert_equal(0, @simple.rindex(/h/, 0))
      assert_equal(2, @simple.rindex(/ll./, 3))
      assert_nil(@simple.rindex(/ll./, 0))
      assert_nil(@simple.rindex(/z./, 0))
   end

   def test_rindex_expected_errors
      assert_raises(TypeError){ @simple.rindex(1..3) }
      assert_raises(ArgumentError){ @simple.rindex(1,2,3) }
   end

   def teardown
      @simple  = nil
      @complex = nil
   end
end
