###############################################################################
# tc_trap.rb
#
# Test case for the Signal.trap class method.
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_Signal_Trap_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file   = 'test_signal_trap.txt'
      @io     = File.new(@file, 'w+')
      @sig1   = WINDOWS ? 'ABRT' : 'USR1'
      @sig2   = WINDOWS ? 'ILL'  : 'USR2'
      @signum = Signal.list[@sig1]
      @proc   = proc{ @io.print 'world' }
      
      # These are the traps. We look to @io or @proc to see if they fired
      # properly within the tests themselves.
      Signal.trap(@sig1){ @io.print('hello') }
      Signal.trap(@sig2, @proc)
   end

   def test_trap_basic
      assert_respond_to(Signal, :trap)
   end

   def test_trap
      assert_nothing_raised{ Process.kill(@sig1, Process.pid) }
      @io.rewind
      assert_equal('hello', @io.read)
   end
   
   def test_trap_with_full_sig_name
      assert_nothing_raised{ Process.kill('SIG' + @sig1, Process.pid) }
      @io.rewind
      assert_equal('hello', @io.read)
   end
   
   def test_trap_with_number
      assert_nothing_raised{ Process.kill(@signum, Process.pid) }
      @io.rewind
      assert_equal('hello', @io.read)
   end

   def test_trap_with_proc
      assert_nothing_raised{ Process.kill(@sig2, Process.pid) }
      @io.rewind
      assert_equal('world', @io.read)
   end
   
   def test_trap_with_sig_symbol
      assert_nothing_raised{ Process.kill(@sig1.to_sym, Process.pid) }
      @io.rewind
      assert_equal('hello', @io.read)
   end
   
   def test_trap_expected_errors
      assert_raise(ArgumentError){ Signal.trap(@signum, 'USR1', 'USR2') }
      assert_raise(ArgumentError){ Signal.trap(999999){} }
   end

   def teardown
      @io.close unless @io.closed?
      remove_file(@file)
      @file   = nil
      @io     = nil
      @signum = nil
      @sig1   = nil
      @sig2   = nil
   end
end
