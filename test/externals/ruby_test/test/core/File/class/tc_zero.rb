#####################################################################
# tc_zero.rb
#
# Test case for the File.zero? class method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Zero_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @zero_file    = 'temp1.txt'
      @nonzero_file = 'temp2.txt'
      
      touch(@zero_file)
      touch(@nonzero_file, "hello")
   end
   
   def test_zero_basic
      assert_respond_to(File, :zero?)
      assert_nothing_raised{ File.zero?(@zero_file) }
   end
   
   def test_zero
      assert_equal(true, File.zero?(@zero_file))
      assert_equal(false, File.zero?(@nonzero_file))
   end
   
   def test_zero_edge_cases
      if WINDOWS
         assert_equal(true, File.zero?('NUL'))
      else
         assert_equal(true, File.zero?('/dev/null'))
      end
   end
   
   def test_zero_expected_failures
      assert_raises(ArgumentError){ File.zero? }
      assert_raises(TypeError){ File.zero?(nil) }
      assert_raises(TypeError){ File.zero?(true) }
      assert_raises(TypeError){ File.zero?(false) }
   end
   
   def teardown
      remove_file(@zero_file)
      remove_file(@nonzero_file)
      
      @zero_file    = nil
      @nonzero_file = nil
   end
end