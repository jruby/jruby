###############################################################################
# tc_each_byte.rb
#
# Test case for the IO#each_byte instance method.
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_EachByte_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file   = 'test_io_each_byte.txt'
      @handle = File.new(@file, 'w+')
      @array  = []
      @handle << "Hello\n"
      @handle.rewind
   end

   def test_each_byte_basic
      assert_respond_to(@handle, :each_byte)
      assert_nothing_raised{ @handle.each_byte{} }
   end
   
   def test_each_byte
      assert_nothing_raised{ @handle.each_byte{ |b| @array << b } }
      assert_equal(72, @array[0])
      assert_equal(101, @array[1])
      assert_equal(108, @array[2])
      assert_equal(108, @array[3])
      assert_equal(111, @array[4])
      assert_equal(10, @array[5])
      assert_nil(@array[6])
   end
   
   def test_each_byte_expected_errors
      assert_raise(ArgumentError){ @handle.each_byte(2){ } }
   end
   
   def teardown
      @handle.close if @handle && !@handle.closed?
      remove_file(@file)
      @array  = nil
      @handle = nil
      @file   = nil
   end
end
