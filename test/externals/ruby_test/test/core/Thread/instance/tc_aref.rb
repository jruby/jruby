########################################################################
# tc_aref.rb
#
# Test case for the Thread#[] instance method.
########################################################################
require 'test/unit'

class TC_Thread_Aref_InstanceMethod < Test::Unit::TestCase
   def setup
      @thread = Thread.new{ Thread.current['name'] = 'A'; Thread.stop }
   end
   
   def test_aref_basic
      assert_respond_to(@thread, :[])
      assert_nothing_raised{ Thread.list.each{ |t| t['name'] } }
   end
   
   def test_aref
#      assert_equal('A', @thread['name'])
#      assert_equal('A', @thread[:name])
      assert_nil(@thread['bogus'])
   end
   
   def test_aref_expected_errors
      assert_raise(ArgumentError){ @thread[] }
      assert_raise(ArgumentError){ @thread['name', 1] }
   end
   
   def teardown
      @thread.exit
      @thread = nil
   end
end