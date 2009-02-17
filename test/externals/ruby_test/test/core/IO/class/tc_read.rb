######################################################################
# tc_read.rb
#
# Test case for the IO.read class method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Read_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = File.join(Dir.pwd, 'tc_read_class.txt')
      File.open(@file, 'wb+'){ |fh|
         fh.puts "hello"
         fh.puts "world"
      }
   end

   def test_read_basic
      assert_respond_to(IO, :read)
      assert_nothing_raised{ IO.read(@file) }
   end

   # The duplicate test here is intentional. We want to verify that the
   # file pointer is definitely reset between reads.
   #
   def test_read
      assert_equal("hello\nworld\n", IO.read(@file))
      assert_equal("hello\nworld\n", IO.read(@file))
   end

   def test_read_with_length
      assert_equal("hello", IO.read(@file, 5))
      assert_equal("hello\nworld\n", IO.read(@file))
   end

   def test_read_with_length_and_offset
      assert_equal("wo", IO.read(@file, 2, 6))
      assert_equal("world\n", IO.read(@file, nil, 6))
   end

   def test_read_explicit_nil
      assert_equal("hello\nworld\n", IO.read(@file, nil))
      assert_equal("hello\nworld\n", IO.read(@file, nil, nil))
   end

   def test_read_zero_length
      assert_equal('', IO.read(@file, 0))
      assert_equal('', IO.read(@file, 0, 100))
   end

   # NOTE: It seems to me that you ought to be able to specify a negative
   # index in the same manner that IO#seek + SEEK_END works. As of 1.8.6 this
   # is not supported, however.
   #
   # NOTE: The last assertion has been commented out because of a handle
   # leak. See RubyForge bug #15065.
   #
   def test_read_expected_errors
      assert_raise(TypeError){ IO.read(@file, "\n") }
      assert_raise(ArgumentError){ IO.read(@file, -2) }
      #assert_raise_kind_of(SystemCallError){ IO.read(@file, 2, -3) }
   end

   def teardown
      remove_file(@file)
   end
end
