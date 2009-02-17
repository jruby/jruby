############################################################
# tc_count.rb
#
# Test case for the String#count instance method.
############################################################
require "test/unit"

class TC_String_Count_Instance < Test::Unit::TestCase
   def setup
      @str = "<html><b>Hello</b></html>\r\n\t"
      @hob = "hel-[()]-lo012^"
   end

   def test_count_basic
      assert_respond_to(@str, :count)
      assert_nothing_raised{ @str.count("l") }
      assert_nothing_raised{ @str.count("hello","^l") }
   end

   def test_count
      assert_equal(2, @str.count("h"))
      assert_equal(1, @str.count("H"))
      assert_equal(4, @str.count("l"))
      assert_equal(4, @str.count("<"))
      assert_equal(4, @str.count(">"))
      assert_equal(2, @str.count("/"))
      assert_equal(1, @str.count("\n"))
      assert_equal(1, @str.count("\r"))
      assert_equal(1, @str.count("\t"))
      assert_equal(0, @str.count(""))     # note
   end

   def test_count_negation
      assert_equal(0, @str.count("b","^b"))
      assert_equal(6, @str.count("html","^l"))
      assert_equal(2, @str.count("\r\n\t","^\n"))
   end

   def test_count_sequence
      assert_equal(6, @str.count("l-m"))
      assert_equal(0, @str.count("m-l")) # note
      assert_equal(4, @str.count("-l"))
   end
   
   # Inspired by JRUBY-1720
   def test_count_high_order_bytes
      assert_equal(1, @hob.count('['))
      assert_equal(1, @hob.count('^'))
      assert_equal(15, @hob.count("\x00-\xFF")) 
   end

   def test_count_expected_errors
      assert_raises(ArgumentError){ @str.count }
      assert_raises(TypeError){ @str.count(1) }
   end

   def teardown
      @str = nil
      @hob = nil
   end
end
