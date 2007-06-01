######################################################################
# tc_sysopen.rb
#
# Test case for the IO.sysopen class method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Sysopen_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   def setup
      @file = 'test.txt'
      touch_n(@file)
   end

   def test_sysopen
      assert_respond_to(IO, :sysopen)
      assert_nothing_raised{ IO.sysopen(@file) }
      assert_kind_of(Fixnum, IO.sysopen(@file))
      assert_equal(true, IO.sysopen(@file) > 2)
   end

   # Duplicate intentional
   #
   def test_sysopen_with_mode
      assert_nothing_raised{ IO.sysopen(@file, 'w') }
      assert_nothing_raised{ IO.sysopen(@file, 'w') }
   end

   def test_sysopen_with_mode_and_perms
      assert_nothing_raised{ IO.sysopen(@file, 'w', 0755) }
   end

   def test_sysopen_expected_errors
      assert_raise(ArgumentError){ IO.sysopen }
      assert_raise(ArgumentError){ IO.sysopen(@file, 'w', 0755, true) }
      assert_raise(TypeError){ IO.sysopen(nil) }
      assert_raise_kind_of(SystemCallError){ IO.sysopen('bogus') }
   end

   def teardown
      remove_file(@file)
   end
end
