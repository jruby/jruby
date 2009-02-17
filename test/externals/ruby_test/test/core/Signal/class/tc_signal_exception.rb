###############################################################################
# tc_signal_exception.rb
#
# Because the SignalException class is defined specially in signal.c (or
# error.c) I give it some extra attention here.
#
# These tests are skipped on versions of Ruby older than 1.8.6. See RubyForge
# bug #11795 for more information.
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_Signal_Exception_Class < Test::Unit::TestCase
   include Test::Helper
    
   def setup
      @sig_name  = WINDOWS ? 'ABRT' : 'USR1'
      @full_name = 'SIG' + @sig_name
      @sig_num   = Signal.list[@sig_name]
      @sig_exc   = SignalException.new(@sig_num)
   end

   # This is really just to ensure that the test suite doesn't raise an error
   # for lack of tests.
   #
   def test_signal_exception_exists
      assert_not_nil(SignalException)
   end
   
   if RELEASE >= 6
      def test_signal_exception_basic
#         assert_respond_to(@sig_exc, :signo)
#         assert_respond_to(@sig_exc, :signm)
      end
   
#      def test_signal_exception_signm
#         assert_equal(@full_name, @sig_exc.signm)
#         assert_equal(@full_name, SignalException.new(@full_name).signm)
#         assert_equal(@sig_name, SignalException.new(@sig_name).signm)
#      end

      def test_signal_exception_signo
         # assert_equal(@sig_num, @sig_exc.signo) # Maybe
      end

      def test_signal_constructor
         assert_nothing_raised{ SignalException.new(@full_name) }
         assert_nothing_raised{ SignalException.new(@full_name.to_sym) }
         assert_nothing_raised{ SignalException.new(@sig_name) }
         assert_nothing_raised{ SignalException.new(@sig_name.to_sym) }
      end

      def test_signal_constructor_expected_errors
         # assert_raise(ArgumentError){ SignalException.new(1,2) } # bug
         assert_raise(ArgumentError){ SignalException.new(1,2,3) }
#         assert_raise(ArgumentError){ SignalException.new(-1) }
#         assert_raise(ArgumentError){ SignalException.new(99999) }
#         assert_raise(ArgumentError){ SignalException.new('foo') }
#         assert_raise(TypeError){ SignalException.new({1,2}) }
      end
   end
   
   def teardown
      @sig_exc   = nil
      @sig_num   = nil
      @sig_name  = nil
      @full_name = nil
   end
end
