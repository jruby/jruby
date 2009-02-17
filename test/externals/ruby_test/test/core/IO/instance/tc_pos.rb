###############################################################################
# tc_pos.rb
#
# Test case for the IO#pos instance method, as well as the IO#tell alias.
###############################################################################
require 'test/unit'

class TC_IO_Pos_InstanceMethod < Test::Unit::TestCase
   def setup
      @file   = 'test_pos.rb'
      @handle = File.new(@file, 'wb+')
      @handle.print("hello\n")
      @handle.rewind
   end

   def test_pos_basic
      assert_respond_to(@handle, :pos)
      assert_nothing_raised{ @handle.pos }
      assert_kind_of(Fixnum, @handle.pos)
   end

   def test_tell_alias_basic
      assert_respond_to(@handle, :tell)
      assert_nothing_raised{ @handle.tell }
      assert_kind_of(Fixnum, @handle.tell)
   end

   def test_pos
      assert_equal(0, @handle.pos)
      assert_nothing_raised{ @handle.getc }
      assert_equal(1, @handle.pos)
      assert_nothing_raised{ @handle.gets }
      assert_equal(6, @handle.pos)
      assert_nothing_raised{ @handle.gets } # Try again...
      assert_equal(6, @handle.pos)          # And make sure it didn't move
   end

   def test_tell_alias
      assert_equal(0, @handle.tell)
      assert_nothing_raised{ @handle.getc }
      assert_equal(1, @handle.tell)
      assert_nothing_raised{ @handle.gets }
      assert_equal(6, @handle.tell)
      assert_nothing_raised{ @handle.gets } # Try again...
      assert_equal(6, @handle.tell)          # And make sure it didn't move
   end

   def test_pos_after_rewind
      assert_nothing_raised{ @handle.getc }
      assert_equal(1, @handle.pos)
      assert_nothing_raised{ @handle.rewind }
      assert_equal(0, @handle.pos)
   end

   def test_tell_alias_after_rewind
      assert_nothing_raised{ @handle.getc }
      assert_equal(1, @handle.tell)
      assert_nothing_raised{ @handle.rewind }
      assert_equal(0, @handle.tell)
   end

   def test_pos_expected_errors
      assert_raise(ArgumentError){ @handle.pos(3) }
   end

   def test_tell_alias_expected_errors
      assert_raise(ArgumentError){ @handle.tell(3) }
   end

   def teardown
      @handle.close if @handle && !@handle.closed?
      File.delete(@file) if File.exists?(@file)
      @handle = nil
      @file   = nil
   end
end
