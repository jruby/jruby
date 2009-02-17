########################################################################
# tc_send.rb
#
# Test case for the Object#send instance method and the Object#__send__
# alias.
########################################################################
require 'test/unit'

class TC_Object_Send_InstanceMethod < Test::Unit::TestCase
   def setup
      @object = Object.new
      @array  = [1, 2, 3]
   end

   def test_send_basic
      assert_respond_to(@object, :send)
   end

   def test_send_strings_or_symbols
      assert_nothing_raised{ @object.send('class') }
      assert_nothing_raised{ @object.send('class'.to_sym) }
   end

   def test_send_method_with_no_arguments_or_block
      assert_nothing_raised{ @object.send(:class) }
      assert_equal(Object, @object.send(:class))
   end

   def test_send_method_with_arguments_but_no_block
      assert_nothing_raised{ @object.send(:==, @object) }
      assert_equal(true, @object.send(:==, @object))
   end

   def test_send_method_with_no_arguments_but_with_block
      assert_nothing_raised{ @array.send(:map!){ |n| n += 1 } }
      assert_equal([2, 3, 4], @array)
   end

   def test_send_with_arguments_and_block
      assert_nothing_raised{ @array.fill(-3){ |e| e * 100 } }
      assert_equal([0, 100, 200], @array)
   end

   def test_send_edge_cases
      assert_nothing_raised{ @object.send(:send, :class) }
   end

   def test_send_expected_failures
      assert_raise(ArgumentError){ @object.send }
      assert_raise(ArgumentError, NoMethodError){ @object.send(:send, '') }
      assert_raise(TypeError){ @object.send(true) }
      assert_raise(TypeError){ @object.send(false) }
      assert_raise(TypeError){ @object.send(nil) }
   end

   def test_send_expected_failures_caused_by_bad_arguments
      assert_raise(ArgumentError){ @array.send(:map, 1) }
   end

   def teardown
      @object = nil
      @array  = nil
   end
end
