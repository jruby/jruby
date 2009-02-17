###############################################################################
# tc_getc.rb
#
# Test case for the IO#getc instance method.
###############################################################################
require 'test/unit'

class TC_IO_Getc_InstanceMethod < Test::Unit::TestCase
   def setup
      @file   = 'test_getc.txt'
      @handle = File.new(@file, 'wb+')
      @handle.print "wo\tr\nld5"
      @handle.rewind
   end

   def test_getc_basic
      assert_respond_to(@handle, :getc)
      assert_nothing_raised{ @handle.getc }
      assert_kind_of(Fixnum, @handle.getc)
   end

   # We'll call it once for each character, plus two more to verify
   # that it returns nil at eof.
   #
   def test_getc
      assert_equal(119, @handle.getc)
      assert_equal(111, @handle.getc)
      assert_equal(9, @handle.getc)
      assert_equal(114, @handle.getc)
      assert_equal(10, @handle.getc)
      assert_equal(108, @handle.getc)
      assert_equal(100, @handle.getc)
      assert_equal(53, @handle.getc)
      assert_equal(nil, @handle.getc)
      assert_equal(nil, @handle.getc)
   end

   def test_getc_expected_errors
      assert_raise(ArgumentError){ @handle.getc(1) }
   end

   def teardown
      @handle.close if @handle && !@handle.closed?
      File.delete(@file) if File.exists?(@file)
      @file   = nil
      @handle = nil
   end
end
