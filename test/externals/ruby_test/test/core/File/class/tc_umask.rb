##########################################################################
# tc_umask.rb
#
# Test case for the File.umask class method. The tests for MS Windows
# are limited, since it only supports two values (read-only or not).
# 
# TODO: Add tests for UNIX platforms.
##########################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Umask_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @file = 'temp1.txt'
      File.open(@file, 'w'){}
      @omask = get_umask
   end
   
   def test_umask_basic
      assert_respond_to(File, :umask)
      assert_nothing_raised{ File.umask }
      assert_kind_of(Fixnum, File.umask)
   end
   
   if WINDOWS
      def test_umask_default    
         assert_equal(0, File.umask)
      end
      
      # The value used here is the value of _S_IWRITE.
      def test_umask_set
         assert_nothing_raised{ File.umask(0000200) }
         assert_equal(128, File.umask)
      end
      
      def test_umask_invalid_values
         assert_nothing_raised{ File.umask(0006) }
         assert_equal(0, File.umask)
      end
   end
   
   def teardown
      remove_file(@file)
      @file = nil
      set_umask(@omask)
   end
end
