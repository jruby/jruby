###########################################################
# tc_index.rb
#
# Test suite for the String#index instance method.
###########################################################
require "test/unit"

class TC_String_Index_Instance < Test::Unit::TestCase
   def setup
      @simple  = "hello"
      @complex = "p1031'/> <b><c n='field'/><c n='fl"
   end

   def test_index_basic
      assert_respond_to(@simple, :index)
      assert_nothing_raised{ @simple.index(0) }
      assert_nothing_raised{ @simple.index("h") }
      assert_nothing_raised{ @simple.index(/\w+/) }
   end

   # This test added due to a bug report (ruby-core:6721) where more
   # complex substrings would return nil on 64 bit platforms.
   #
   def test_index_string_complex
      assert_equal(9, @complex.index("<b><c n='field'/><c n='fl"))
   end

   def test_index_string
      assert_equal(0, @simple.index("h"))
      assert_equal(1, @simple.index("ell"))
      assert_nil(@simple.index("z"))
   end

   def test_index_string_with_offset
      assert_equal(3, @simple.index("l",3))
      assert_nil(@simple.index("l",4))
      assert_nil(@simple.index("h",1))
      assert_nil(@simple.index("z",1))
      assert_nil(@simple.index("z",99))
   end

   def test_index_int
      assert_equal(0, @simple.index(?h))
      assert_equal(1, @simple.index(?e))
      assert_nil(@simple.index(?z))
   end

   def test_index_int_with_offset
      assert_equal(0, @simple.index(?h,0))
      assert_equal(3, @simple.index(?l,3))
      assert_equal(1, @simple.index(?e,0))
      assert_nil(@simple.index(?l,4))
      assert_nil(@simple.index(?h,1))
      assert_nil(@simple.index(?z,0))
   end

   def test_index_regex
      assert_equal(0, @simple.index(/h/))
      assert_equal(2, @simple.index(/ll./))
      assert_nil(@simple.index(/z./))
   end

   def test_index_regex_with_offset
      assert_equal(0, @simple.index(/h/, 0))
      assert_equal(2, @simple.index(/ll./, 0))
      assert_nil(@simple.index(/ll./, 3))
      assert_nil(@simple.index(/h/, 1))
      assert_nil(@simple.index(/z./, 0))
   end

   def test_index_expected_errors
      assert_raises(TypeError){ @simple.index(1..3) }
      assert_raises(ArgumentError){ @simple.index(1,2,3) }
   end

   def teardown
      @simple  = nil
      @complex = nil
   end
end
