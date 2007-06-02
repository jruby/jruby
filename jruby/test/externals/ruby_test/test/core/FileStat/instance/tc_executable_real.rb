######################################################################
# tc_executable_real.rb
#
# Test case for the FileStat#executable_real? instance method.
######################################################################
require 'test/unit'

class TC_FileStat_ExecutableReal_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_executable_real_basic
      assert_respond_to(@stat, :executable_real?)
   end

   def test_executable_real
      assert_equal(false, @stat.executable_real?)
   end

   def test_executable_real_expected_errors
      assert_raises(ArgumentError){ @stat.executable_real?(1) }
   end

   def teardown
      @stat = nil
   end
end
