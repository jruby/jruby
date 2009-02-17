######################################################################
# tc_pipe.rb
#
# Test case for the FileStat#pipe instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_Pipe_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stat = File::Stat.new(__FILE__)
      @file = WINDOWS ? 'NUL' : '/dev/stdin'
   end

   def test_pipe_basic
      assert_respond_to(@stat, :pipe?)
   end

   def test_pipe
      assert_equal(false, @stat.pipe?)
#      assert_equal(false, File::Stat.new(@file).pipe?)
   end

   def test_pipe_expected_errors
      assert_raises(ArgumentError){ @stat.pipe?(1) }
   end

   def teardown
      @stat = nil
      @file = nil
   end
end
