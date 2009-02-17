######################################################################
# tc_file.rb
#
# Test case for the FileStat#file? instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_File_Instance < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stat = File::Stat.new(__FILE__)
      if WINDOWS
         @file = 'NUL'
      else
         @file = '/dev/stdin'
      end
   end

   def test_file_basic
      assert_respond_to(@stat, :file?)
   end

   # Windows seems to return true no matter what.  I consider it a bug.
   def test_file
      assert_equal(true, @stat.file?)
      assert_equal(false, File::Stat.new(@file).file?) unless WINDOWS
   end

   def test_file_expected_errors
      assert_raises(ArgumentError){ @stat.file?(1) }
   end

   def teardown
      @stat = nil
      @file = nil
   end
end
