######################################################################
# tc_identical.rb
#
# Test case for the File.identical? class method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Identical_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file1 = 'test_identical1.txt'
      @file2 = 'test_identical2.txt'
      @file3 = 'test_identical3.txt'

      touch_n(@file1)
      touch_n(@file2)

      File.link(@file1, @file3)
   end

   def test_identical_basic
      assert_respond_to(File, :identical?)
      assert_nothing_raised{ File.identical?(@file1, @file2) }
      assert_kind_of(Boolean, File.identical?(@file1, @file2))
   end

   def test_identical
      assert_equal(true, File.identical?(@file1, @file1))
      assert_equal(false, File.identical?(@file1, @file2))
      assert_equal(true, File.identical?(@file1, @file3))
   end

   def test_identical_expected_errors
      assert_raises(ArgumentError){ File.identical?(@file1, @file2, @file3) }
      assert_raises(TypeError){ File.identical?(1,1) }
   end

   def teardown
      remove_file(@file1)
      remove_file(@file2)
      remove_file(@file3)
      @file1 = nil
      @file1 = nil
      @file1 = nil
   end
end
