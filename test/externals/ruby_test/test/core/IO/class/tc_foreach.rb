######################################################################
# tc_foreach.rb
#
# Test case for the IO.foreach class method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Foreach_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = 'test.txt'
      File.open(@file, 'w+'){ |fh|
         fh.puts "hello"
         fh.puts "world"
      }
      @array = []
   end

   def test_foreach_basic
      assert_respond_to(IO, :foreach)
      assert_nothing_raised{ IO.foreach(@file){} }
   end

   def test_foreach
      assert_nothing_raised{ IO.foreach(@file){ |line| @array << line } }
      assert_equal("hello\n", @array[0])
      assert_equal("world\n", @array[1])
      assert_nil(@array[2])
   end

   def test_foreach_with_null_separator
      assert_nothing_raised{ IO.foreach(@file, ""){ |line| @array << line } }
      assert_equal("hello\nworld\n", @array[0])
      assert_nil(@array[1])
   end

   def test_foreach_with_separator
      assert_nothing_raised{ IO.foreach(@file, "l"){ |line| @array << line } }
      assert_equal("hel", @array[0])
      assert_equal("l", @array[1])
      assert_equal("o\nworl", @array[2])
      assert_equal("d\n", @array[3])
      assert_nil(@array[4])
   end

   def test_foreach_expected_errors
      assert_raise(ArgumentError){ IO.foreach }
      assert_raise(ArgumentError){ IO.foreach(@file, '', 1) }
   # No longer a valid test in 1.8.7
=begin
      assert_raise(LocalJumpError){ IO.foreach(@file) }
      assert_raise(TypeError){ IO.foreach(@file, 1) }
      assert_raise(TypeError){ IO.foreach(55) }
=end
   end

   def teardown
      remove_file(@file)
      @array = nil
   end
end
