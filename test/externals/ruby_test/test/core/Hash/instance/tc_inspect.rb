###############################################################################
# tc_inspect.rb
#
# Test case for the Hash#inspect instance method. There's a custom inspect
# implementation in hash.c, so that's what we test it instead of relying on
# the tests for Object#inspect.
###############################################################################
require 'test/unit'

class TC_Hash_Inspect_InstanceMethod < Test::Unit::TestCase
   def setup
      @empty     = {}
      @normal    = {1, 'a', 2, 'b'}
      @recursive = {1, 'a'}
      @recursive.store(@recursive, 2)
      @recursive.store(3, @recursive)
   end

   def test_inspect_basic
      assert_respond_to(@empty, :inspect)
      assert_nothing_raised{ @empty.inspect }
      assert_kind_of(String, @empty.inspect)
   end

   def test_inspect_empty
      assert_equal('{}', @empty.inspect)
   end

   def test_inspect_normal
      assert_equal('{1=>"a", 2=>"b"}', @normal.inspect)
   end

   # There's no way to sort this, so we'll check for any
   def test_inspect_recursive
      possible = [
         '{1=>"a", {...}=>2, 3=>{...}}',
         '{1=>"a", 3=>{...}, {...}=>2}',
         '{3=>{...}, {...}=>2, 1=>"a"}',
         '{3=>{...}, 1=>"a", {...}=>2}',
         '{{...}=>2, 3=>{...}, 1=>"a"}',
         '{{...}=>2, 1=>"a", 3=>{...}}',
      ]
      assert_equal(true, possible.include?(@recursive.inspect))
   end

   def test_inspect_expected_errors
      assert_raise(ArgumentError){ @normal.inspect(true) }
   end

   def teardown
      @empty     = nil
      @normal    = nil
      @recursive = nil
   end
end
