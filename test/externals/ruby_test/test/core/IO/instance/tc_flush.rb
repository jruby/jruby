###############################################################################
# tc_flush.rb
#
# Test case for the IO#flush instance method.
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Flush_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file   = 'io_flush_test.txt'
      @handle = File.open(@file, 'w+')
   end

   def test_flush_basic
      assert_respond_to(@handle, :flush)
      assert_nothing_raised{ @handle.flush }
      assert_kind_of(IO, @handle.flush)
   end

   def test_flush
      assert_nothing_raised{ @handle.print 'hello' }
      assert_nothing_raised{ @handle.flush }
      assert_equal('hello', IO.read(@file))
   end
   
   def test_flush_expected_errors
      assert_raise(ArgumentError){ @handle.flush(5) }
   end

   def teardown
      @handle.close unless @handle.closed?
      remove_file(@file)
   end
end
