###############################################################################
# tc_errno.rb
#
# Test case for the Errno module. The tests here are rather generic because
# it is impossible to predict which Errno classes will exist on any given
# system.
###############################################################################
require 'test/unit'

class TC_Errno_Module < Test::Unit::TestCase
   def test_errno_constants_basic
      assert_respond_to(Errno, :constants)
      assert_kind_of(Array, Errno.constants)
   end

   def test_errno_subclass_of_systemcallerror
      assert_kind_of(SystemCallError, Errno::EINVAL.new)
      assert_kind_of(SystemCallError, Errno::EACCES.new)
   end

   def test_errno_constant
      assert_kind_of(Fixnum, Errno::EINVAL::Errno)
   end
end
