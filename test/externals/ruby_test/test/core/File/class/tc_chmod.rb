#####################################################################
# tc_chmod.rb
#
# Test case for the File.chmod class method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Chmod_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file1 = File.expand_path(__FILE__)
      @file2 = base_file(__FILE__, 'tc_atime.rb')
      @file1_mode = File.stat(@file1).mode
      @file2_mode = File.stat(@file2).mode
   end

   def test_chmod_basic
      assert_respond_to(File, :chmod)
      assert_nothing_raised{ File.chmod(0644, @file1) }
      assert_nothing_raised{ File.chmod(0644, @file1, @file2) }
      assert_kind_of(Fixnum, File.chmod(0644, @file1))
   end

   def test_chmod
      assert_nothing_raised{ File.chmod(0644, @file1) }
      assert_equal(1, File.chmod(0644, @file1))
      assert_equal('100644', File.stat(@file1).mode.to_s(8))

      assert_nothing_raised{ File.chmod(0444, @file2) }
      assert_equal(1, File.chmod(0444, @file2))
      assert_equal('100444', File.stat(@file2).mode.to_s(8))
   end

   def test_chmod_multiple_files
      assert_nothing_raised{ File.chmod(0444, @file1, @file2) }
      assert_equal(2, File.chmod(0444, @file1, @file2))
      assert_equal('100444', File.stat(@file1).mode.to_s(8))
      assert_equal('100444', File.stat(@file2).mode.to_s(8))
   end

   def test_chmod_edge_cases
#      assert_nothing_raised{ File.chmod(0444) } # Debatable
#      assert_equal(0, File.chmod(0444))
   end

   def test_chmod_expected_errors
      assert_raises(ArgumentError){ File.chmod }
#      assert_raises(TypeError){ File.chmod('0644') }
   end

   def teardown
      File.chmod(@file1_mode, @file1)
      File.chmod(@file2_mode, @file2)
      @file1 = nil
      @file2 = nil
      @file1_mode = nil
      @file2_mode = nil
   end
end
