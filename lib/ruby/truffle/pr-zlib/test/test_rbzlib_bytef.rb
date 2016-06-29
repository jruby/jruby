########################################################################
# test_rbzlib_bytef.rb
#
# Test case for the Zlib::Bytef class.
########################################################################
require 'pr/rbzlib'
require 'test-unit'

class TC_Rbzlib_Bytef < Test::Unit::TestCase
   def setup
      @buffer = 0.chr * 32
      @bytef  = Rbzlib::Bytef.new(@buffer)
   end

   def test_buffer_get
      assert_respond_to(@bytef, :buffer)
      assert_equal(@buffer, @bytef.buffer)
   end

   def test_buffer_set
      assert_respond_to(@bytef, :buffer=)
      assert_nothing_raised{ @bytef.buffer = 0.chr * 8 }
      assert_equal(0.chr * 8, @bytef.buffer)
   end

   def test_length
      assert_respond_to(@bytef, :length)
      assert_equal(32, @bytef.length)
   end

   def test_increment
      assert_respond_to(@bytef, :+)
      assert_nothing_raised{ @bytef + 1 }
      assert_equal(1, @bytef.offset)
   end

   def test_decrement
      assert_respond_to(@bytef, :-)
      assert_nothing_raised{ @bytef - 1 }
      assert_equal(-1, @bytef.offset)
   end

   def test_aref
      assert_respond_to(@bytef, :[])
      assert_nothing_raised{ @bytef[1] }
      assert_equal(0, @bytef[1])
   end

   def test_aset
      assert_respond_to(@bytef, :[]=)
      assert_nothing_raised{ @bytef[1] = 1.chr }
      assert_equal(1, @bytef[1])
   end

   def test_get
      assert_respond_to(@bytef, :get)
      assert_nothing_raised{ @bytef.get }
      assert_equal(0, @bytef.get)
   end

   def test_set
      assert_respond_to(@bytef, :set)
      assert_nothing_raised{ @bytef.set('a') }
      assert_equal(97, @bytef.get)
   end

   def test_current
      assert_respond_to(@bytef, :current)
      assert_equal(@buffer, @bytef.current)
   end

   def teardown
      @bytef  = nil
      @buffer = nil
   end
end
