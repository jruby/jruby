#####################################################################
# tc_constants.rb
#
# Test case that verifies that certain constants within the File
# class are defined and, in some cases, their values.
#
# Note that there is some overlap between this test case and the
# equivalent test case for IO.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Constants < Test::Unit::TestCase
   include Test::Helper
   
   def test_match_mode_constants
      assert_not_nil(File::FNM_NOESCAPE)
      assert_not_nil(File::FNM_PATHNAME)
      assert_not_nil(File::FNM_DOTMATCH)
      assert_not_nil(File::FNM_CASEFOLD)
      assert_not_nil(File::FNM_SYSCASE)

      if WINDOWS || VMS
         assert_equal(8, File::FNM_SYSCASE)
      end
   end

   # Only these constants are not inherited from the IO class
   def test_separator_constants
      assert_not_nil(File::SEPARATOR)
      assert_not_nil(File::Separator)
      assert_not_nil(File::PATH_SEPARATOR)
      assert_equal("/", File::SEPARATOR)

      if WINDOWS || VMS
         assert_not_nil(File::ALT_SEPARATOR) 
         assert_equal(";", File::PATH_SEPARATOR)
      else
         assert_nil(File::ALT_SEPARATOR) 
         assert_equal(":", File::PATH_SEPARATOR)
      end
   end

   def test_open_mode_constants
      assert_not_nil(File::APPEND)
      assert_not_nil(File::CREAT)
      assert_not_nil(File::EXCL)
      assert_not_nil(File::NONBLOCK)
      assert_not_nil(File::RDONLY)
      assert_not_nil(File::RDWR)
      assert_not_nil(File::TRUNC)
      assert_not_nil(File::WRONLY)
      
      unless WINDOWS # Not sure about VMS here
         assert_not_nil(File::NOCTTY)
      end
   end

   def test_lock_mode_constants
      assert_not_nil(File::LOCK_EX)
      assert_not_nil(File::LOCK_NB)
      assert_not_nil(File::LOCK_SH)
      assert_not_nil(File::LOCK_UN)
   end
end
