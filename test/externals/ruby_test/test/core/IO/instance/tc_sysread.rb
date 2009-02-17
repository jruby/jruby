######################################################################
# tc_sysread.rb
#
# Test case for the IO#sysread instance method. These tests are
# nearly identical to the IO#read tests.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Sysread_InstancMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = File.join(Dir.pwd, 'tc_sysread.txt')

      @handle = File.open(@file, 'wb+')
      @handle.puts "hello"
      @handle.puts "world"
      @handle.rewind

      @buffer = ""
   end

   def test_sysread_basic
      assert_respond_to(@handle, :sysread)
      assert_nothing_raised{ @handle.sysread(1) }
      assert_nothing_raised{ @handle.sysread(1000) }
   end

   def test_sysread
      assert_equal('h', @handle.sysread(1))
      assert_equal('ello', @handle.sysread(4))
      assert_equal("\nworld\n", @handle.sysread(400))
   end

   def test_sysread_into_string_buffer
      assert_equal('hello', @handle.sysread(5, @buffer))
      assert_equal('hello', @buffer)
      assert_equal("\nworld\n", @handle.sysread(7, @buffer))
      assert_equal("\nworld\n", @buffer)
   end

   def test_sysread_eof_error
      assert_nothing_raised{ @handle.sysread(1000) }
      assert_raise(EOFError){ @handle.sysread(1) }
   end

   def test_sysread_expected_errors
      assert_raise(TypeError){ @handle.sysread('foo') }
      assert_raise(TypeError){ @handle.sysread(1, 7) }
   end

   def teardown
      @handle.close if @handle && !@handle.closed?
      remove_file(@file)
      @file   = nil
      @buffer = nil
      @handle = nil
   end
end
