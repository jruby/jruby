######################################################################
# tc_grpowned.rb
#
# Test case for the FileStat#grpowned? instance method.
######################################################################
require 'test/unit'

class TC_FileStat_GrpOwned_Instance < Test::Unit::TestCase
   WINDOWS = RUBY_PLATFORM.match('mswin')
   
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_grpowned_basic
      assert_respond_to(@stat, :grpowned?)
   end

   def test_grpowned
      if WINDOWS
         assert_equal(false, @stat.grpowned?)
      else
         assert_equal(true, @stat.grpowned?)
         assert_equal(false, File::Stat.new('/').grpowned?)
      end
   end

   def test_grpowned_expected_errors
      assert_raises(ArgumentError){ @stat.grpowned?(1) }
   end

   def teardown
      @stat = nil
   end
end
