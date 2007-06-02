######################################################################
# tc_read.rb
#
# Test case for the Dir#read instance method.
######################################################################
require "test/unit"

class TC_Dir_Read_Instance < Test::Unit::TestCase
   def setup
      @dir = Dir.new(Dir.pwd)
   end

   def test_read_basic
      assert_respond_to(@dir, :read)
      assert_nothing_raised{ @dir.read }
   end

   def test_read
      assert_equal(".", @dir.read)
      assert_equal("..", @dir.read)
      assert_nothing_raised{ Dir.entries(Dir.pwd).length.times{ @dir.read } }
      assert_equal(nil, @dir.read)
   end

   def test_read_expected_errors
      assert_raises(ArgumentError){ @dir.read(2) }
   end

   def teardown
      @dir = nil
   end
end
