###############################################################################
# tc_pos_set.rb
#
# Test case for the IO#pos= instance method.
###############################################################################
require 'test/unit'

class TC_IO_PosSet_InstanceMethod < Test::Unit::TestCase
   def setup
      @file   = 'test_pos_set.rb'
      @handle = File.new(@file, 'wb+')
      @handle.print("hello\n")
      @handle.rewind
   end

   def test_pos_set_basic
      assert_respond_to(@handle, :pos=)
      assert_nothing_raised{ @handle.pos = 0 }
      assert_kind_of(Fixnum, @handle.pos = 0)
   end

   def test_pos_set
      assert_equal(0, @handle.pos)
      assert_equal(4, @handle.pos = 4)
      assert_equal(4, @handle.pos)
      assert_equal(0, @handle.pos = 0)
   end

   def test_pos_set_edge_cases
      assert_nothing_raised{ @handle.pos = 99 }
      assert_equal(99, @handle.pos)
   end

   def test_pos_set_expected_errors
      assert_raise(ArgumentError){ @handle.send(:pos=, 1, 2) }
   end

   def teardown
      @handle.close if @handle && !@handle.closed?
      File.delete(@file) if File.exists?(@file)
      @handle = nil
      @file   = nil
   end
end
