######################################################################
# tc_read.rb
#
# Test case for the IO#read instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Read_InstancMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = File.join(Dir.pwd, 'tc_read.txt')

      @handle = File.open(@file, 'wb+')
      @handle.puts "hello"
      @handle.puts "world"
      @handle.rewind

      @buffer = ""
   end

   def test_read_basic
      assert_respond_to(@handle, :read)
      assert_nothing_raised{ @handle.read(1) }
      assert_nothing_raised{ @handle.read(1000) }
   end

   def test_read
      assert_equal('h', @handle.read(1))
      assert_equal('ello', @handle.read(4))
      assert_equal("\nworld\n", @handle.read(400))
   end

   def test_read_into_string_buffer
      assert_equal('hello', @handle.read(5, @buffer))
      assert_equal('hello', @buffer)
      assert_equal("\nworld\n", @handle.read(7, @buffer))
      assert_equal("\nworld\n", @buffer)
   end

   def test_read_nil_at_eof
      assert_nothing_raised{ @handle.read(1000) }
      assert_nil(@handle.read(1))
   end

   def test_read_zero_length
      assert_equal('', @handle.read(0))
      assert_equal('', @handle.read(0, @buffer))
      assert_equal('', @buffer)
   end

   def test_read_buffer_overwritten
      @buffer = 'test12345'
      assert_equal('hello', @handle.read(5, @buffer))
      assert_equal('hello', @buffer)
   end

   def test_read_expected_errors
      assert_raise(TypeError){ @handle.read('foo') }
      assert_raise(TypeError){ @handle.read(1, 7) }
      assert_raise(ArgumentError){ @handle.read(-1) }
   end

   def teardown
      @handle.close if @handle && !@handle.closed?
      remove_file(@file)
      @file   = nil
      @buffer = nil
      @handle = nil
   end
end
