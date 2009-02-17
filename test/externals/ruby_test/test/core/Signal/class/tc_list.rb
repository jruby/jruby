###############################################################################
# tc_list.rb
#
# Test case for the Signal.list class method.
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_Signal_List_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @list = nil
   end

   def test_list_basic
      assert_respond_to(Signal, :list)
      assert_nothing_raised{ Signal.list }
      assert_kind_of(Hash, Signal.list)
   end

   def test_list
      @list = Signal.list

      if WINDOWS
         assert_equal(true, @list.has_key?('KILL'))
      else
         assert_equal(true, @list.has_key?('ALRM'))
         assert_equal(true, @list.has_key?('BUS'))
         assert_equal(true, @list.has_key?('CHLD'))
         assert_equal(true, @list.has_key?('HUP'))
         assert_equal(true, @list.has_key?('KILL'))
         assert_equal(true, @list.has_key?('TERM'))
      end
   end

   def test_list_values_numeric
      Signal.list.each{ |key, val| assert_kind_of(Fixnum, val) }
   end

   def test_list_expected_errors
      assert_raise(ArgumentError){ Signal.list(true) }
   end

   def teardown
      @list = nil
   end
end
