#####################################################################
# tc_exist.rb
#
# Test case for the File.exist? class method and the File.exists?
# alias.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Exist_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = 'temp.txt'
      touch(@file)
   end

   def test_exist_basic
      assert_respond_to(File, :exist?)
      assert_nothing_raised{ File.exist?(@file) }
   end

   def test_exist
      assert_equal(true, File.exist?(@file))
      assert_equal(false, File.exist?('bogus'))
   end

   def test_exists_alias
      assert_respond_to(File, :exists?)
      assert_equal(true, File.exists?(@file))
      assert_equal(false, File.exists?('bogus'))
   end

   def test_exist_expected_errors
      assert_raises(ArgumentError){ File.exist? }
      assert_raises(ArgumentError){ File.exist?(@file, @file) }
      assert_raises(TypeError){ File.exist?(nil) }
   end

   def teardown
      remove_file(@file)
      @file = nil
   end
end
