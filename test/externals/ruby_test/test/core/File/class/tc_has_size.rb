########################################################################
# tc_has_size.rb
#
# Test case for the File.size? class method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_File_HasSize_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file_zero    = 'tc_has_size_zero.txt'
      @file_nonzero = 'tc_has_size_nonzero.txt'
      touch(@file_zero)
      touch(@file_nonzero, 'Test for File.size?')
   end

   def test_has_size_basic
      assert_respond_to(File, :size?)
      assert_nothing_raised{ File.size?(@file_zero) }
      assert_nothing_raised{ File.size?(@file_nonzero) }
   end

   def test_has_size
      assert_equal(nil, File.size?(@file_zero))
      assert_equal(20, File.size?(@file_nonzero))
   end

   def test_has_size_on_nonexistent_file
      assert_equal(nil, File.size?('bogus.txt'))
   end

   def test_has_size_expected_errors
      assert_raise(ArgumentError){ File.size?(@file_zero, 1) }
      assert_raise(TypeError){ File.size?(1) }
   end

   def teardown
      File.delete(@file_zero) if File.exists?(@file_zero)
      File.delete(@file_nonzero) if File.exists?(@file_nonzero)

      @file_zero    = nil
      @file_nonzero = nil
   end
end
