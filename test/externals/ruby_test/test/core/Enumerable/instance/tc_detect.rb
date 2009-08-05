#########################################################################
# tc_detect.rb
#
# Test suite for the Enumerable#detect instance method and the
# Enumerable#find alias.
#########################################################################
require 'test/unit'

class MyEnumDetect
   include Enumerable

   attr_accessor :arg1, :arg2, :arg3

   def initialize(arg1=1, arg2=2, arg3=3)
      @arg1 = arg1
      @arg2 = arg2
      @arg3 = arg3
   end

   def each
      yield @arg1
      yield @arg2
      yield @arg3
   end
end

class TC_Enumerable_Detect_InstanceMethod < Test::Unit::TestCase
   def setup
      @enum   = MyEnumDetect.new
      @lambda = lambda{ 'test' }
   end

   def test_detect_basic
      assert_respond_to(@enum, :detect)
      assert_nothing_raised{ @enum.detect{} }
   end

   def test_detect
      assert_equal(2, @enum.detect{ |e| e > 1 })
      assert_equal(nil, @enum.detect{ |e| e > 7 })
   end

   def test_detect_with_proc_argument
      assert_equal(2, @enum.detect(@lambda){ |e| e > 1 })
      assert_equal('test', @enum.detect(@lambda){ |e| e > 7 })
   end

   def test_detect_edge_cases
      assert_equal(1, @enum.detect{ true })
      assert_equal(nil, @enum.detect{})
   end

   # No longer a valid test in 1.8.7
=begin
   def test_find_alias
      msg = "=> Known issue in MRI"
      assert_respond_to(@enum, :find)
      assert_equal(true, @enum.method(:find) == @enum.method(:detect), msg)
   end
=end

   def test_expected_errors
   # No longer a valid test in 1.8.7
=begin
      assert_raise(LocalJumpError){ @enum.detect }
=end
      assert_raise(ArgumentError){ @enum.detect(5, 7) }
      assert_raise(NoMethodError){ @enum.detect('test'){ |e| e > 7 } }
   end

   def teardown
      @enum   = nil
      @lambda = nil
   end
end
