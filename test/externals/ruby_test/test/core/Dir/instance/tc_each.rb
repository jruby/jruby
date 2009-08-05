######################################################################
# tc_each.rb
#
# Test case for the Dir#each instance method.
######################################################################
require "test/unit"
require "test/helper"

class TC_Dir_Each_Instance < Test::Unit::TestCase
   include Test::Helper

   def setup
      @pwd = pwd_n
      @dir = Dir.new(@pwd)
      if WINDOWS
         @entries = `dir /A /B`.split("\n").push('.', '..')
      else
         @entries = `ls -a1`.split("\n")
         @entries.push('.') unless @entries.include?('.')
         @entries.push('..') unless @entries.include?('..')
      end
   end

   def test_each_basic
      assert_respond_to(@dir, :each)
      assert_nothing_raised{ @dir.each{} }
   end

   def test_each
      array = []
      assert_nothing_raised{ @dir.each{ |dir| array.push(dir) }}
      assert_kind_of(String, array.first)
      assert_kind_of(String, array.last)
      assert_equal(@entries.sort, array.sort)
   end

   def test_each_expected_errors
   # No longer a valid test in 1.8.7
=begin
      assert_raises(LocalJumpError){ @dir.each }
=end
      assert_raises(ArgumentError){ @dir.each(1){} }
   end

   def teardown
      @dir     = nil
      @pwd     = nil
      @entries = nil
   end
end
