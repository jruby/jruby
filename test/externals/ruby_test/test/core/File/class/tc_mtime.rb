#####################################################################
# tc_mtime.rb
#
# Test case for the File.mtime class method.
#####################################################################
require 'test/unit'

class TC_File_Mtime_ClassMethod < Test::Unit::TestCase
   def setup
      @file = __FILE__
   end

   def test_mtime
      assert_respond_to(File, :mtime)
      assert_nothing_raised{ File.mtime(@file) }
      assert_kind_of(Time, File.mtime(@file))
   end

   def test_mtime_expected_errors
      assert_raises(Errno::ENOENT){ File.mtime('bogus') }
      assert_raises(ArgumentError){ File.mtime }
      assert_raises(ArgumentError){ File.mtime(@file, @file) }
      assert_raises(TypeError){ File.mtime(1) }
   end

   def teardown
      @file = nil
   end
end
