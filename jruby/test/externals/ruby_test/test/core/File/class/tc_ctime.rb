#####################################################################
# tc_ctime.rb
#
# Test case for the File.ctime class method.
#####################################################################
require 'test/unit'

class TC_File_Ctime_ClassMethod < Test::Unit::TestCase
   def setup
      @file = __FILE__
   end

   def test_ctime
      assert_respond_to(File, :ctime)
      assert_nothing_raised{ File.ctime(@file) }
      assert_kind_of(Time, File.ctime(@file))
   end

   def test_ctime_expected_errors
      assert_raises(Errno::ENOENT){ File.ctime('bogus') }
      assert_raises(ArgumentError){ File.ctime }
      assert_raises(ArgumentError){ File.ctime(@file, @file) }
      assert_raises(TypeError){ File.ctime(1) }
   end

   def teardown
      @file = nil
   end
end
