#####################################################################
# tc_and.rb
#
# Test case for 'false &'. We use the stringio class here to verify
# the difference in handling between '&' and '&&'.
#####################################################################
require 'test/unit'
require 'stringio'

class TC_FalseClass_And_InstanceMethod < Test::Unit::TestCase
   def setup
      @sio = StringIO.new
   end

   def lookup(val)
      @sio.write(val)
      @sio.rewind
   end

   def test_and_basic
      assert_respond_to(false, :&)
      assert_nothing_raised{ false.&(0) }
      assert_nothing_raised{ false & 0 }
   end

   def test_single_and
      assert_equal(false, false & 0)
      assert_equal(false, false & 1)
      assert_equal(false, false & true)
      assert_equal(false, false & false)

      assert_nothing_raised{ false & lookup('cat') }
      assert_equal('cat', @sio.read(100))
   end

   def test_double_and
      assert_equal(false, false && 0)
      assert_equal(false, false && 1)
      assert_equal(false, false && true)
      assert_equal(false, false && false)

      assert_nothing_raised{ false && lookup('cat') }
      assert_equal(nil, @sio.read(100))
   end

   def teardown
      @sio = nil
   end
end
