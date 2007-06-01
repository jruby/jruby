#########################################################################
# tc_size.rb
#
# Test case for the FileStat#size and File::Stat#size? instance methods.
#########################################################################
require 'test/unit'

class TC_FileStat_Size_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
      @file = RUBY_PLATFORM.match('mswin') ? 'NUL' : '/dev/null'
   end

   def test_size_basic
      assert_respond_to(@stat, :size)
      assert_respond_to(@stat, :size?)
   end

   def test_size
      assert_kind_of(Fixnum, @stat.size)
      assert_equal(true, @stat.size > 0)
   end

   def test_size_bool
      assert_not_nil(@stat.size?)
      assert_nil(File::Stat.new(@file).size?)
   end

   def test_size_expected_errors
      assert_raises(ArgumentError){ @stat.size?(1) }
   end

   def teardown
      @stat = nil
      @file = nil
   end
end
