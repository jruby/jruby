######################################################################
# tc_readlines.rb
#
# Test case for the IO.readlines class method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Readlines_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = 'test.txt'
      File.open(@file, 'w+'){ |fh|
         fh.puts "hello"
         fh.puts "world"
      }
   end

   def test_readlines_basic
      assert_respond_to(IO, :readlines)
      assert_nothing_raised{ IO.readlines(@file) }
      assert_kind_of(Array, IO.readlines(@file))
   end

   # The duplicate test here is intentional. We want to verify that the
   # file pointer is definitely reset between readliness.
   #
   def test_readlines
      assert_equal(["hello\n", "world\n"], IO.readlines(@file))
      assert_equal(["hello\n", "world\n"], IO.readlines(@file))
   end

   def test_readlines_with_separator
      assert_equal(["hello\nworld\n"], IO.readlines(@file, nil))
      assert_equal(["hell", "o\nworld\n"], IO.readlines(@file, "ll"))
      assert_equal(["hel","l", "o\nworl", "d\n"], IO.readlines(@file, "l"))
      assert_equal(["hello", "\nworld\n"], IO.readlines(@file, "hello"))
   end

   def test_readlines_expected_errors
      assert_raise(TypeError){ IO.readlines(2) }
      assert_raise(TypeError){ IO.readlines(@file, -5) }
   end

   def teardown
      remove_file(@file)
   end
end
