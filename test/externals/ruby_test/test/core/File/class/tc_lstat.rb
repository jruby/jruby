########################################################################
# tc_lstat.rb
#
# Test case for the File.lstat method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Lstat_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file  = __FILE__
      @lstat = nil
      @null  = null_device

      unless WINDOWS
         @link = 'link_to_tc_lstat.rb'
         File.symlink(__FILE__, @link)
      end
   end

   def test_lstat_basic
      assert_respond_to(File, :lstat)
   end

   def test_lstat
      assert_nothing_raised{ @lstat = File.lstat(@file) }
      assert_kind_of(File::Stat, File.lstat(@file))
   end

   unless WINDOWS
      def test_lstat_symlink
         assert_nothing_raised{ @lstat = File.lstat(@link) }
         assert_equal(true, @lstat.symlink?)
      end
   end

   def test_lstat_edge_cases
      assert_nothing_raised{ File.lstat(@null) }
   end

   # Do some superficial testing of the File::Stat object.
   def test_lstat_instance_methods
      assert_nothing_raised{ @lstat = File.lstat(@file) }
      assert_respond_to(@lstat, :atime)
      assert_respond_to(@lstat, :blksize)
      assert_respond_to(@lstat, :blockdev?)
      assert_respond_to(@lstat, :directory?)
      assert_respond_to(@lstat, :size)
   end

   def test_lstat_expected_errors
      assert_raise(Errno::ENOENT){ File.lstat('bogus') }
      assert_raise(Errno::ENOENT){ File.lstat('') }
      assert_raise(ArgumentError){ File.lstat }
      assert_raise(ArgumentError){ File.lstat(@file, @file) }
      assert_raise(TypeError){ File.lstat(1) }
   end

   def teardown
      unless WINDOWS
         File.delete(@link) if File.exists?(@link)
      end
      @file = nil
      @lstat = nil
      @null = nil
   end
end
