###############################################################################
# tc_readchar.rb
#
# Test case for the IO#readchar instance method. These tests are nearly
# identical to the IO#getc tests.
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Readchar_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file   = 'test_readchar.txt'
      @handle = File.new(@file, 'wb+')
      @handle.print "wo\tr\nld5"
      @handle.rewind
   end

   def test_readchar_basic
      assert_respond_to(@handle, :readchar)
      assert_nothing_raised{ @handle.readchar }
      assert_kind_of(Fixnum, @handle.readchar)
   end

   # We'll call it once for each character
   def test_readchar
      assert_equal(119, @handle.readchar)
      assert_equal(111, @handle.readchar)
      assert_equal(9, @handle.readchar)
      assert_equal(114, @handle.readchar)
      assert_equal(10, @handle.readchar)
      assert_equal(108, @handle.readchar)
      assert_equal(100, @handle.readchar)
      assert_equal(53, @handle.readchar)
   end

   def test_readchar_expected_errors
      assert_raise(ArgumentError){ @handle.readchar(1) }
      assert_raise(EOFError){ 10.times{ @handle.readchar } }
   end

   def teardown
      @handle.close if @handle && !@handle.closed?
      File.delete(@file) if File.exists?(@file)
      @file   = nil
      @handle = nil
   end
end
