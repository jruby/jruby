######################################################################
# tc_executable.rb
#
# Test case for the FileStat#executable? instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Executable_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_executable_basic
      assert_respond_to(@stat, :executable?)
   end

   def test_executable
      assert_equal(false, @stat.executable?)
   end

   def test_executable_expected_errors
      assert_raises(ArgumentError){ @stat.executable?(1) }
   end

   def teardown
      @stat = nil
   end
end
