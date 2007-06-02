######################################################################
# tc_ino.rb
#
# Test case for the FileStat#ino instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_Ino_Instance < Test::Unit::TestCase
   include Test::Helper

   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_ino_basic
      assert_respond_to(@stat, :ino)
      assert_kind_of(Fixnum, @stat.ino)
   end

   def test_ino
      if WINDOWS
         assert_equal(false, @stat.ino > 0)
      else
         assert_equal(true, @stat.ino > 0)
      end
   end

   def test_ino_expected_errors
      assert_raises(ArgumentError){ @stat.ino(1) }
      assert_raises(NoMethodError){ @stat.ino = 1 }
   end

   def teardown
      @stat = nil
   end
end
