######################################################################
# tc_new.rb
#
# Test case for the File.new class method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_File_New_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @fh    = nil
      @file  = 'test_file_new.txt'
      @flags = File::CREAT | File::TRUNC | File::WRONLY
      touch_n(@file)
   end

   def test_new_basic
      assert_respond_to(File, :new)
      assert_nothing_raised{ @fh = File.new(@file) }
      assert_kind_of(File, @fh)
      assert_equal(true, File.exists?(@file))
   end

   def test_new_with_mode_string
      assert_nothing_raised{ @fh = File.new(@file, 'w') }
      assert_kind_of(File, @fh)
      assert_equal(true, File.exists?(@file))
   end

   def test_new_with_mode_num
      assert_nothing_raised{ @fh = File.new(@file, @flags) }
      assert_kind_of(File, @fh)
      assert_equal(true, File.exists?(@file))
   end

   # MS Windows only supports two file modes - 0644 (read-write) and 0444
   # (read-only). So, we'll verify that those two work, and that it defaults
   # to the expected 0644 for invalid modes.
   #
   if WINDOWS
      def test_new_with_mode_num_and_permissions_ms_windows_read_write
         assert_nothing_raised{ File.delete(@file) }
         assert_nothing_raised{ @fh = File.new(@file, @flags, 0644) }
         assert_kind_of(File, @fh)
         assert_equal("100644", File.stat(@file).mode.to_s(8))
         assert_equal(true, File.exists?(@file))
      end

      def test_new_with_mode_num_and_permissions_ms_windows_read_only
         assert_nothing_raised{ File.delete(@file) }
         assert_nothing_raised{ @fh = File.new(@file, @flags, 0444) }
         assert_kind_of(File, @fh)
         assert_equal("100444", File.stat(@file).mode.to_s(8))
         assert_equal(true, File.exists?(@file))
      end

      def test_new_with_mode_num_and_permissions_ms_windows_invalid_mode
         assert_nothing_raised{ File.delete(@file) }
         assert_nothing_raised{ @fh = File.new(@file, @flags, 0777) }
         assert_kind_of(File, @fh)
         assert_equal("100644", File.stat(@file).mode.to_s(8))
         assert_equal(true, File.exists?(@file))
      end
   else
      def test_new_with_mode_num_and_permissions
         assert_nothing_raised{ File.delete(@file) }
         assert_nothing_raised{ @fh = File.new(@file, @flags, 0755) }
         assert_kind_of(File, @fh)
         assert_equal("100755", File.stat(@file).mode.to_s(8))
         assert_equal(true, File.exists?(@file))
      end
   end

   def test_new_with_fd
      assert_nothing_raised{ @fh = File.new(@file) }
      assert_nothing_raised{ @fh = File.new(@fh.fileno) }
      assert_kind_of(File, @fh)
      assert_equal(true, File.exists?(@file))
   end

   def test_new_expected_errors
      assert_raise(TypeError){ File.new(true) }
      assert_raise(TypeError){ File.new(false) }
      assert_raise(TypeError){ File.new(nil) }
      assert_raise_kind_of(SystemCallError){ File.new(-1) }
      assert_raise(ArgumentError){ File.new(@file, File::CREAT, 0755, 'test') }
   end

   # You can't alter mode or permissions when opening a file descriptor.
   # TODO: Except on Windows apparently - find out what happens on MS Windows.
   #
   # Update: It seems other platforms allow it, too, such as Solaris 10. So,
   # I'm going to comment that test out for now until I've done further
   # research.
   #
   def test_new_with_fd_expected_errors
      assert_nothing_raised{ @fh = File.new(@file) }
      #assert_raise(Errno::EINVAL){ File.new(@fh.fileno, @flags) } unless WINDOWS
   end

   def teardown
      @fh.close if @fh
      remove_file(@file)
      @fh    = nil
      @file  = nil
      @flags = nil
   end
end
