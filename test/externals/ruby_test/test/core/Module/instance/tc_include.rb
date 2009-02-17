########################################################################
# tc_include.rb
#
# Test case for the Module#include? instance method.
########################################################################
require 'test/unit'
require 'test/helper'

module IncludeA
end

class IncludeB
   include IncludeA
end

class IncludeC < IncludeB
end

class TC_Module_Include_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def test_include_basic
      assert_respond_to(IncludeC, :include?)
      assert_nothing_raised{ IncludeC.include?(IncludeA) }
      assert_kind_of(Boolean, IncludeC.include?(IncludeA))
   end

   def test_include
      assert_equal(true, IncludeB.include?(IncludeA))
      assert_equal(true, IncludeC.include?(IncludeA))
      assert_equal(false, IncludeA.include?(IncludeA))
   end

   def test_include_expected_errors
      assert_raise(ArgumentError){ IncludeC.include? }
      assert_raise(ArgumentError){ IncludeC.include?(IncludeA, IncludeA) }
      assert_raise(TypeError){ IncludeC.include?(IncludeB) }
      assert_raise(TypeError){ IncludeC.include?(true) }
      assert_raise(TypeError){ IncludeC.include?(nil) }
   end
end
