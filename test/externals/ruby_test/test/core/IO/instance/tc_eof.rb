######################################################################
# tc_eof.rb
#
# Test case for the IO#eof method and the IO#eof? alias.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Eof_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file   = 'test_eof.txt'
      @handle = File.new(@file, 'w+')
   end

   def test_eof_basic
      assert_respond_to(@handle, :eof)
      assert_respond_to(@handle, :eof?)
      assert_kind_of(Boolean, @handle.eof)
   end

   # A handle to an empty file returns true for IO#eof?. So, we test that
   # first, then write some data to it, rewind the handle and test again.
   # Then, we read from the handle and test yet again.
   #
   def test_eof
      assert_equal(true, @handle.eof?)
      assert_nothing_raised{ @handle.puts("hello") }
      assert_nothing_raised{ @handle.rewind }
      assert_equal(false, @handle.eof?)
      assert_nothing_raised{ @handle.read }
      assert_equal(true, @handle.eof?)
   end

   def test_eof_expected_failures
      assert_raise(ArgumentError){ @handle.eof(1) }
      assert_raise(ArgumentError){ @handle.eof?(1) }
   end

   def teardown
      @handle.close if @handle && !@handle.closed?
      remove_file(@file)
      @file = nil
   end
end
