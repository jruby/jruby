######################################################################
# tc_open.rb
#
# Test case for the File.open class method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Open_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = 'tc_open.txt'
      @fh = nil
      @fd = nil
      @flags = File::CREAT | File::TRUNC | File::WRONLY
      touch_n(@file)
   end

   def test_open_basic
      assert_respond_to(File, :open)
      assert_nothing_raised{ @fh = File.open(@file) }
      assert_kind_of(File, @fh)
      assert_equal(true, File.exists?(@file))
   end

   def test_open_basic_with_block
      assert_nothing_raised{ File.open(@file){ |fh| @fd = fh.fileno } }
      assert_raise_kind_of(SystemCallError){ File.open(@fd) } # Should be closed by block
      assert_equal(true, File.exists?(@file))
   end

   def test_open_with_mode_string
      assert_nothing_raised{ @fh = File.open(@file, 'w') }
      assert_kind_of(File, @fh)
      assert_equal(true, File.exists?(@file))
   end

   def test_open_with_mode_string_and_block
      assert_nothing_raised{ File.open(@file, 'w'){ |fh| @fd = fh.fileno } }
      assert_raise_kind_of(SystemCallError){ File.open(@fd) }
      assert_equal(true, File.exists?(@file))
   end

   def test_open_with_mode_num
      assert_nothing_raised{ @fh = File.open(@file, @flags) }
      assert_kind_of(File, @fh)
      assert_equal(true, File.exists?(@file))
   end

   def test_open_with_mode_num_and_block
      assert_nothing_raised{ File.open(@file, 'w'){ |fh| @fd = fh.fileno } }
      assert_raise_kind_of(SystemCallError){ File.open(@fd) }
      assert_equal(true, File.exists?(@file))
   end

   # For this test we delete the file first to reset the permissions.
   #
   # MS Windows only supports two file modes - 0644 (read-write) and 0444
   # (read-only). So, we'll verify that those two work, and that it defaults
   # to the expected 0644 for invalid modes.
   #
   if WINDOWS
      def test_new_with_mode_num_and_permissions_ms_windows_read_write
         assert_nothing_raised{ File.delete(@file) }
         assert_nothing_raised{ @fh = File.open(@file, @flags, 0644) }
         assert_kind_of(File, @fh)
         assert_equal("100644", File.stat(@file).mode.to_s(8))
         assert_equal(true, File.exists?(@file))
      end

      def test_new_with_mode_num_and_permissions_ms_windows_read_only
         assert_nothing_raised{ File.delete(@file) }
         assert_nothing_raised{ @fh = File.open(@file, @flags, 0444) }
         assert_kind_of(File, @fh)
         assert_equal("100444", File.stat(@file).mode.to_s(8))
         assert_equal(true, File.exists?(@file))
      end

      def test_new_with_mode_num_and_permissions_ms_windows_invalid_mode
         assert_nothing_raised{ File.delete(@file) }
         assert_nothing_raised{ @fh = File.open(@file, @flags, 0777) }
         assert_kind_of(File, @fh)
         assert_equal("100644", File.stat(@file).mode.to_s(8))
         assert_equal(true, File.exists?(@file))
      end
   else
      def test_open_with_mode_num_and_permissions
         assert_nothing_raised{ File.delete(@file) }
         assert_nothing_raised{ @fh = File.open(@file, @flags, 0755) }
         assert_equal("100755", File.stat(@file).mode.to_s(8))
         assert_kind_of(File, @fh)
         assert_equal(true, File.exists?(@file))
      end
   end

   # For this test we delete the file first to reset the perms
   if WINDOWS
      def test_new_with_mode_num_and_permissions_ms_windows_read_write_block
         assert_nothing_raised{ File.delete(@file) }
         assert_nothing_raised{
            File.open(@file, @flags, 0644){ |fh| @fd = fh.fileno }
         }
         assert_raise_kind_of(SystemCallError){ File.open(@fd) }
         assert_equal("100644", File.stat(@file).mode.to_s(8))
         assert_equal(true, File.exists?(@file))
      end

      def test_new_with_mode_num_and_permissions_ms_windows_read_only_block
         assert_nothing_raised{ File.delete(@file) }
         assert_nothing_raised{
            File.open(@file, @flags, 0444){ |fh| @fd = fh.fileno }
         }
         assert_raise_kind_of(SystemCallError){ File.open(@fd) }
         assert_equal("100444", File.stat(@file).mode.to_s(8))
         assert_equal(true, File.exists?(@file))
      end

      def test_new_with_mode_num_and_permissions_ms_windows_invalid_mode_block
         assert_nothing_raised{ File.delete(@file) }
         assert_nothing_raised{
            File.open(@file, @flags, 0755){ |fh| @fd = fh.fileno }
         }
         assert_raise_kind_of(SystemCallError){ File.open(@fd) }
         assert_equal("100644", File.stat(@file).mode.to_s(8))
         assert_equal(true, File.exists?(@file))
      end
   else
      def test_open_with_mode_num_and_permissions_and_block
         assert_nothing_raised{ File.delete(@file) }
         assert_nothing_raised{
            File.open(@file, @flags, 0755){ |fh| @fd = fh.fileno }
         }
         assert_raise_kind_of(SystemCallError){ File.open(@fd) }
         assert_equal("100755", File.stat(@file).mode.to_s(8))
         assert_equal(true, File.exists?(@file))
      end
   end

   def test_open_with_fd
      assert_nothing_raised{ @fh = File.open(@file) }
      assert_nothing_raised{ @fh = File.open(@fh.fileno) }
      assert_kind_of(File, @fh)
      assert_equal(true, File.exists?(@file))
   end

   # Note that this test invalidates the file descriptor in @fh. That's
   # handled in the teardown via the 'rescue nil'.
   #
   def test_open_with_fd_and_block
      assert_nothing_raised{ @fh = File.open(@file) }
      assert_nothing_raised{ File.open(@fh.fileno){ |fh| @fd = fh.fileno } }
      assert_raise_kind_of(SystemCallError){ File.open(@fd) }
      assert_kind_of(File, @fh)
      assert_equal(true, File.exists?(@file))
   end

   def test_open_expected_errors
      assert_raise(TypeError){ File.open(true) }
      assert_raise(TypeError){ File.open(false) }
      assert_raise(TypeError){ File.open(nil) }
      assert_raise_kind_of(SystemCallError){ File.open(-1) }
      assert_raise(ArgumentError){ File.open(@file, File::CREAT, 0755, 'test') }
   end

   def teardown
      begin
         @fh.close if @fh && !@fh.closed?
      rescue Errno::EBADF
         # Do nothing. Caused by test_open_with_fd_and_block
      end
      remove_file(@file)
      @fh    = nil
      @fd    = nil
      @file  = nil
      @flags = nil
   end
end
