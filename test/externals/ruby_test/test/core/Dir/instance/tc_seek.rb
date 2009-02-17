######################################################################
# tc_seek.rb
#
# Test case for the Dir#seek instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Dir_Seek_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @pwd = pwd_n
      @dir = Dir.new(@pwd)
   end

   def test_seek_basic
      assert_respond_to(@dir, :seek)
      assert_nothing_raised{ @dir.seek(0) }
   end

   # This caused a segfault on Windows for some versions of Ruby.
   def test_seek
      assert_equal(".", @dir.read)
      assert_kind_of(Dir, @dir.seek(0))
   end

   def test_seek_expected_errors
      assert_raises(TypeError){ @dir.seek("bogus") }
      assert_raises(ArgumentError){ @dir.seek(0,0) }
   end

   def teardown
      @pwd = nil
      @dir = nil
   end
end
