######################################################################
# tc_mode.rb
#
# Test case for the FileStat#mode instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Mode_Instance < Test::Unit::TestCase
   def setup
      File.chmod(0644, __FILE__)
      @stat = File::Stat.new(__FILE__)
   end

   def test_mode_basic
      assert_respond_to(@stat, :mode)
      assert_kind_of(Fixnum, @stat.mode)
   end

   def test_mode
      assert_equal(33188, @stat.mode)
      assert_equal("100644", @stat.mode.to_s(8))
   end

   def test_mode_expected_errors
      assert_raises(ArgumentError){ @stat.mode(1) }
      assert_raises(NoMethodError){ @stat.mode = 1 }
   end

   def teardown
      @stat = nil
   end
end
