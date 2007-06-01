#####################################################################
# tc_or.rb
#
# Test case for 'true |'. We use the stringio class here to verify
# the difference in handling between '|' and '||'.
#####################################################################
require 'test/unit'
require 'stringio'

class TC_TrueClass_Or_InstanceMethod < Test::Unit::TestCase
   def setup
      @sio = StringIO.new
   end

   def lookup(val)
      @sio.write(val)
      @sio.rewind
   end

   def test_or_basic
      assert_respond_to(true, :|)
      assert_nothing_raised{ true.|(0) }
      assert_nothing_raised{ true | 0 }
   end

   def test_single_or
      assert_equal(true, true | 0)
      assert_equal(true, true | 1)
      assert_equal(true, true | true)
      assert_equal(true, true | true)

      assert_nothing_raised{ true | lookup('cat') }
      assert_equal('cat', @sio.read(100))
   end

   def test_double_or
      assert_equal(true, true || 0)
      assert_equal(true, true || 1)
      assert_equal(true, true || true)
      assert_equal(true, true || true)

      assert_nothing_raised{ true || lookup('cat') }
      assert_equal(nil, @sio.read(100))
   end

   def teardown
      @sio = nil
   end
end
