######################################################################
# tc_new.rb
#
# Test case for the IO.new class method, and the IO.for_fd alias.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_New_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @stream = nil
   end

   def test_new_basic
      assert_respond_to(IO, :new)
      assert_respond_to(IO, :for_fd)
   end

   def test_new
      assert_nothing_raised{ @stream = IO.new(2) }
      assert_kind_of(IO, @stream)
   end

   def test_for_fd
      assert_nothing_raised{ @stream = IO.for_fd(2) }
      assert_kind_of(IO, @stream)
   end

   def test_new_with_modestring
      assert_nothing_raised{ @stream = IO.new(2, 'w') }
      assert_kind_of(IO, @stream)
   end

   def test_for_fd_with_modestring
      assert_nothing_raised{ @stream = IO.for_fd(2, 'w') }
      assert_kind_of(IO, @stream)
   end

   def test_new_expected_errors
      assert_raise(ArgumentError){ IO.new(2, 'w', 2) }
      assert_raise(TypeError){ IO.new("test", 'w') }
      assert_raise_kind_of(SystemCallError){ IO.new(999) }
   end

   def test_for_fd_expected_errors
      assert_raise(ArgumentError){ IO.for_fd(2, 'w', 2) }
      assert_raise(TypeError){ IO.for_fd("test", 'w') }
      assert_raise_kind_of(SystemCallError){ IO.for_fd(999) }
   end

   def teardown
      @stream = nil
   end
end
