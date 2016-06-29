########################################################################
# test_rbzlib_posf.rb
#
# Test case for the Zlib::Posf class.
########################################################################
require 'pr/rbzlib'
require 'test-unit'

class TC_Rbzlib_Posf < Test::Unit::TestCase
   def setup
      @buffer = 0.chr * 32
      @posf   = Rbzlib::Posf.new(@buffer)
   end

   def test_increment
      assert_respond_to(@posf, :+)
      assert_nothing_raised{ @posf + 4 }
      assert_equal(8, @posf.offset)
   end

   def test_decrement
      assert_respond_to(@posf, :-)
      assert_nothing_raised{ @posf - 4 }
      assert_equal(-8, @posf.offset)
   end

   def test_aref
      assert_respond_to(@posf, :[])
      assert_nothing_raised{ @posf[2] }
      assert_equal(0, @posf[2])
   end

   def test_aset
      assert_respond_to(@posf, :[]=)
      assert_nothing_raised{ @posf[2] = 7 }
      assert_equal(7, @posf[2])
      assert_equal("\a\000", @posf.buffer[4,2])
   end

   def test_get
      assert_respond_to(@posf, :get)
      assert_nothing_raised{ @posf.get }
      assert_equal(0, @posf.get)
   end

   def test_set
      assert_respond_to(@posf, :set)
      assert_nothing_raised{ @posf.set(4) }
      assert_equal("\004\000", @posf.buffer[0,2])
   end

   def teardown
      @posf   = nil
      @buffer = nil
   end
end
