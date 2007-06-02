######################################################################
# tc_directory.rb
#
# Test case for the FileStat#directory? instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Directory_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_directory_basic
      assert_respond_to(@stat, :directory?)
   end

   def test_directory
      assert_equal(false, @stat.directory?)
      assert_equal(true, File::Stat.new(Dir.pwd).directory?)
      assert_equal(true, File::Stat.new('/').directory?)
   end

   def test_directory_expected_errors
      assert_raises(ArgumentError){ @stat.directory?(1) }
   end

   def teardown
      @stat = nil
   end
end
