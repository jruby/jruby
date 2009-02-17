######################################################################
# tc_delete.rb
#
# Test case for the Dir.delete class method.  This also covers the
# Dir.rmdir and Dir.unlink aliases.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Dir_Delete_Class < Test::Unit::TestCase
   include Test::Helper

   def setup
      @cur_dir = base_dir(__FILE__)
      @new_dir = File.join(@cur_dir, 'bogus')
      Dir.mkdir(@new_dir)
   end

   def test_delete_basic
      assert_respond_to(Dir, :delete)
      assert_nothing_raised{ Dir.delete(@new_dir) }
   end

   def test_rmdir_basic
      assert_respond_to(Dir, :rmdir)
      assert_nothing_raised{ Dir.rmdir(@new_dir) }
   end

   def test_unlink_basic
      assert_respond_to(Dir, :unlink)
      assert_nothing_raised{ Dir.unlink(@new_dir) }
   end

   def test_delete
      assert_equal(0, Dir.delete(@new_dir))
   end

   def test_rmdir
      assert_equal(0, Dir.rmdir(@new_dir))
   end

   def test_unlink
      assert_equal(0, Dir.rmdir(@new_dir))
   end

   def test_delete_expected_errors
      assert_raise(TypeError){ Dir.delete(1) }
      assert_raise(ArgumentError){ Dir.delete(@new_dir, @new_dir) }
      assert_raise_kind_of(SystemCallError){ Dir.delete(@cur_dir) }
   end

   def test_rmdir_expected_errors
      assert_raise(TypeError){ Dir.rmdir(1) }
      assert_raise(ArgumentError){ Dir.rmdir(@new_dir, @new_dir) }
      assert_raise_kind_of(SystemCallError){ Dir.rmdir(@cur_dir) }
   end

   def test_unlink_expected_errors
      assert_raise(TypeError){ Dir.unlink(1) }
      assert_raise(ArgumentError){ Dir.unlink(@new_dir, @new_dir) }
      assert_raise_kind_of(SystemCallError){ Dir.unlink(@cur_dir) }
   end

   def teardown
      remove_dir(@new_dir) if File.exists?(@new_dir)
      @cur_dir = nil
      @new_dir = nil
   end
end
