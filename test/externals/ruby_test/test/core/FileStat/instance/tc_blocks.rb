######################################################################
# tc_blocks.rb
#
# Test case for the FileStat#blocks instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_Blocks_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_blocks_basic
      assert_respond_to(@stat, :blocks)
      if WINDOWS
         assert_nil(@stat.blocks)
      else
         assert_kind_of(Fixnum, @stat.blocks)
      end
   end

   def test_blocks
      unless WINDOWS
         assert_equal(true, @stat.blocks > 0)
      end
   end

   def test_blocks_expected_errors
      assert_raises(ArgumentError){ @stat.blocks(1) }
   end

   def teardown
      @stat = nil
   end
end
