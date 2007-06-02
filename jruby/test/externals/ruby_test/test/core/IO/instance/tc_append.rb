######################################################################
# tc_append.rb
#
# Test case for the IO#<< instance method.
######################################################################
require 'test/unit'
require 'test/helper'

# Because the IO#<< method uses an object's .to_s method, we want
# to verify that an object without a .to_s method will cause a failure
# if we try to pass it as an argument.
#
class AppendFoo
   undef_method :to_s
end

class TC_IO_Append_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file   = 'test_append.txt'
      @handle = File.new(@file, 'w+')
      @appfoo = AppendFoo.new
   end

   def test_append_basic
      assert_respond_to(@handle, :<<)
      assert_nothing_raised{ @handle << "hello" }
      assert_nothing_raised{ @handle << 7 }
      assert_nothing_raised{ @handle << %w/hello world/ }
   end

   def test_append
      assert_equal(0, File.size(@file))
      assert_nothing_raised{ @handle << "hello" }
      assert_nothing_raised{ @handle.close }
      assert_equal(5, File.size(@file))
   end

   def test_append_chained
      assert_equal(0, File.size(@file))
      assert_nothing_raised{ @handle << "hello" << 7 << [1,2,3] }
      assert_nothing_raised{ @handle.close }
      assert_equal(9, File.size(@file))
   end

   def test_append_edge_cases
      assert_nothing_raised{ @handle << nil }
      assert_nothing_raised{ @handle << "" }
      assert_nothing_raised{ @handle << true }
      assert_nothing_raised{ @handle << false }
   end

   def test_append_expected_errors
      assert_raise(ArgumentError){ @handle.send(:<<, 1, 2) }
      assert_raise(NoMethodError){ @handle << @appfoo }
   end

   def teardown
      @appfoo = nil
      @handle.close if @handle rescue nil
      remove_file(@file)
   end
end
