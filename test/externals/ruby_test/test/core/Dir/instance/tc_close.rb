######################################################################
# tc_close.rb
#
# Test case for the Dir#close instance method
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Dir_Close_Instance < Test::Unit::TestCase
   include Test::Helper

   def setup
      @dirname = "bogus"
      system("mkdir #{@dirname}")
      @dir = Dir.new(@dirname)
   end

   def test_close_basic
      assert_respond_to(@dir, :close)
      assert_nothing_raised{ @dir.close }
   end

   def test_close
      assert_equal(nil, @dir.close)
   end

   def test_close_expected_errors
      assert_raises(ArgumentError){ @dir.close(1) }

      @dir.close
      assert_raises(IOError){ @dir.close }
   end

   def teardown
      @dir = nil
      remove_dir(@dirname)
   end
end
