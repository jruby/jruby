#####################################################################
# tc_atime.rb
#
# Test case for the File.atime class method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Atime_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @file = File.join(Dir.pwd, 'test.txt')
      touch(@file)
   end

   def test_atime
      assert_respond_to(File, :atime)
      assert_nothing_raised{ File.atime(@file) }
      assert_kind_of(Time, File.atime(@file))
   end

   def test_atime_expected_errors
      assert_raises(Errno::ENOENT){ File.atime('bogus') }
      assert_raises(ArgumentError){ File.atime }
      assert_raises(ArgumentError){ File.atime(@file, @file) }
      assert_raises(TypeError){ File.atime(1) }
   end

   def teardown
      remove_file(@file)
      @file = nil
   end
end
