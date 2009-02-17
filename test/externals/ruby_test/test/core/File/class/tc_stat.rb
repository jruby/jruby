########################################################################
# tc_stat.rb
#
# Test case for the File.stat method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Stat_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = __FILE__
      @stat = nil
      @null = null_device

      unless WINDOWS
         @link = 'link_to_tc_stat.rb'
         File.symlink(__FILE__, @link)
      end
   end

   def test_stat_basic
      assert_respond_to(File, :stat)
   end

   def test_stat
      assert_nothing_raised{ @stat = File.stat(@file) }
      assert_kind_of(File::Stat, File.stat(@file))
   end

   unless WINDOWS
      def test_stat_symlink
         assert_nothing_raised{ @stat = File.stat(@link) }
         assert_equal(false, @stat.symlink?)
      end
   end

   def test_stat_edge_cases
      assert_nothing_raised{ File.stat(@null) }
   end

   # Do some superficial testing of the File::Stat object.
   def test_stat_instance_methods
      assert_nothing_raised{ @stat = File.stat(@file) }
      assert_respond_to(@stat, :atime)
      assert_respond_to(@stat, :blksize)
      assert_respond_to(@stat, :blockdev?)
      assert_respond_to(@stat, :directory?)
      assert_respond_to(@stat, :size)
   end

   def test_stat_expected_errors
      assert_raise(Errno::ENOENT){ File.stat('bogus') }
      assert_raise(Errno::ENOENT){ File.stat('') }
      assert_raise(ArgumentError){ File.stat }
      assert_raise(ArgumentError){ File.stat(@file, @file) }
      assert_raise(TypeError){ File.stat(1) }
   end

   def teardown
      unless WINDOWS
         File.delete(@link) if File.exists?(@link)
      end
      @file = nil
      @stat = nil
      @null = nil
   end
end
