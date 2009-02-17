########################################################################
# tc_aset.rb
#
# Test case for the Thread#[]= instance method.
########################################################################
require 'test/unit'

class TC_Thread_Aset_InstanceMethod < Test::Unit::TestCase
   def setup
      @thread = Thread.new{ Thread.current['name'] = 'A'; Thread.stop }
   end
   
   def test_aset_basic
      assert_respond_to(@thread, :[]=)
      assert_nothing_raised{ Thread.list.each{ |t| t['name'] = 'test' } }
   end
   
   def test_aset
      assert_equal('test', @thread['name'] = 'test')
#      assert_equal('test', @thread['name'])
#      assert_equal('test', @thread[:name])
   end
   
   def test_aset_expected_errors
      assert_raise(ArgumentError){ @thread.send(:[]=) }
      assert_raise(ArgumentError){ @thread.send(:[]=, 'x') }
      assert_raise(ArgumentError){ @thread.send(:[]=, 'x', 'y', 'z') }
   end
   
   def teardown
      @thread.exit
      @thread = nil
   end
end
