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
      @file  = 'test.txt'
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

   def test_new_with_mode_num_and_permissions
      assert_nothing_raised{ File.delete(@file) }
      assert_nothing_raised{ @fh = File.new(@file, @flags, 0755) }
      assert_kind_of(File, @fh)
      assert_equal("100755", File.stat(@file).mode.to_s(8))
      assert_equal(true, File.exists?(@file))
   end

   def test_new_with_fd
      assert_nothing_raised{ @fh = File.new(@file) }
      assert_nothing_raised{ @fh = File.new(@fh.fileno) }
      assert_kind_of(File, @fh)
      assert_equal(true, File.exists?(@file))
   end

   def test_new_expected_errors
      assert_raises(TypeError){ File.new(true) }
      assert_raises(TypeError){ File.new(false) }
      assert_raises(TypeError){ File.new(nil) }
      assert_raises(Errno::EBADF){ File.new(-1) }
      assert_raises(ArgumentError){ File.new(@file, File::CREAT, 0755, 'test') }
   end

   # You can't alter mode or permissions when opening a file descriptor
   #
   def test_new_with_fd_expected_errors
      assert_nothing_raised{ @fh = File.new(@file) }
      assert_raises(Errno::EINVAL){ File.new(@fh.fileno, @flags) }
   end

   def teardown
      @fh.close if @fh
      remove_file(@file)
      @fh    = nil
      @file  = nil
      @flags = nil
   end
end
