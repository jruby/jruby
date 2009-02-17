###############################################################################
# tc_hierarchy.rb
#
# This test case validates the existence of all the exceptions provided with
# core Ruby, as well as the appropriate subclass hierarchy.
###############################################################################
require 'test/unit'

class TC_Exception_Hierarchy < Test::Unit::TestCase
   def test_exception
      assert_not_nil(Exception)
   end

   def test_no_memory_error
      assert_not_nil(NoMemoryError)
   end

   def test_no_memory_error_hierarchy
      assert_kind_of(Exception, NoMemoryError.new)
   end

   def test_script_error
      assert_not_nil(ScriptError)
      assert_not_nil(LoadError)
      assert_not_nil(NotImplementedError)
      assert_not_nil(SyntaxError)
   end

   def test_script_error_hierarchy
      assert_kind_of(ScriptError, LoadError.new)
      assert_kind_of(ScriptError, NotImplementedError.new)
      assert_kind_of(ScriptError, SyntaxError.new)
   end

   def test_signal_exception
      assert_not_nil(SignalException)
      assert_not_nil(Interrupt)
   end

   def test_signal_exception_hierarchy
      assert_kind_of(SignalException, Interrupt.new(0))
   end

   def test_standard_error
      assert_not_nil(StandardError)
      assert_not_nil(ArgumentError)
      assert_not_nil(IOError)
      assert_not_nil(EOFError)
      assert_not_nil(IndexError)
      assert_not_nil(LocalJumpError)
      assert_not_nil(NameError)
      assert_not_nil(NoMethodError)
      assert_not_nil(RangeError)
      assert_not_nil(FloatDomainError)
      assert_not_nil(RegexpError)
      assert_not_nil(RuntimeError)
      assert_not_nil(SecurityError)
      assert_not_nil(SystemCallError)
      assert_not_nil(ThreadError)
      assert_not_nil(TypeError)
      assert_not_nil(ZeroDivisionError)
   end

   def test_standard_error_hierarchy
      assert_kind_of(StandardError, StandardError.new)
      assert_kind_of(StandardError, IOError.new)
      assert_kind_of(StandardError, IndexError.new)
      assert_kind_of(StandardError, LocalJumpError.new)
      assert_kind_of(StandardError, NameError.new)
      assert_kind_of(StandardError, RangeError.new)
      assert_kind_of(StandardError, RegexpError.new)
      assert_kind_of(StandardError, RuntimeError.new)
      assert_kind_of(StandardError, SecurityError.new)
      assert_kind_of(StandardError, SystemCallError.new(0))
      assert_kind_of(StandardError, ThreadError.new)
      assert_kind_of(StandardError, TypeError.new)
      assert_kind_of(StandardError, ZeroDivisionError.new)
   end

   def test_io_error_hierarchy
      assert_kind_of(IOError, EOFError.new)
   end

   def test_name_error_hierarchy
      assert_kind_of(NameError, NoMethodError.new)
   end

   def tst_range_error_hierarchy
      assert_kind_of(RangeError, FloatDomainError.new)
   end

   def test_system_exit
      assert_not_nil(SystemExit)
   end

   def test_system_exit_hierarchy
      assert_kind_of(Exception, SystemExit.new)
   end

   def test_system_stack_error
      assert_not_nil(SystemStackError)
   end

   def test_system_stack_error_hierarchy
      assert_kind_of(Exception, SystemStackError.new)
   end
end
