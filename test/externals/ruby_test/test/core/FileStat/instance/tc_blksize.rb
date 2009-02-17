######################################################################
# tc_blksize.rb
#
# Test case for the FileStat#blksize instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_Blksize_Instance < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_blksize_basic
      assert_respond_to(@stat, :blksize)
      if WINDOWS
         assert_nil(@stat.blksize)
      else      
         assert_kind_of(Fixnum, @stat.blksize)
      end
   end

   def test_blksize
      unless WINDOWS
         assert_equal(true, @stat.blksize > 0)
         assert_equal(true, @stat.blksize.modulo(512) == 0)
      end
   end

   def test_blksize_expected_errors
      assert_raises(ArgumentError){ @stat.blksize(1) }
      assert_raises(NoMethodError){ @stat.blksize = 1 }
   end

   def teardown
      @stat = nil
   end
end
