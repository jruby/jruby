######################################################################
# tc_foreach.rb
#
# Test case for the Dir.foreach class method.
######################################################################
require "test/unit"
require "test/helper"

class TC_Dir_Foreach_Class < Test::Unit::TestCase
   include Test::Helper

   def setup
      if WINDOWS
         @entries = `dir /A /B`.split("\n").push('.', '..')
      else
         @entries = `ls -a1`.split("\n")
         @entries.push('.') unless @entries.include?('.')
         @entries.push('..') unless @entries.include?('..')
      end
      @pwd = Dir.pwd
   end

   def test_foreach_basic
      assert_respond_to(Dir, :foreach)
      assert_nothing_raised{ Dir.foreach(@pwd){ } }
   end

   def test_foreach
      entries = []
      assert_nothing_raised{ Dir.foreach(@pwd){ |e| entries.push(e) } }
      assert_equal(entries.sort, @entries.sort)
   end

   def test_foreach_expected_errors
      assert_raises(ArgumentError){ Dir.foreach }
      assert_raises(ArgumentError){ Dir.foreach(@pwd, @pwd) }
      assert_raises(TypeError){ Dir.foreach(1){} }
   end

   def teardown
      @pwd     = nil
      @entries = nil
   end
end
