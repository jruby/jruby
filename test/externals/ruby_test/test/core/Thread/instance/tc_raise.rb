########################################################################
# tc_raise.rb
#
# Test case for the Thread#raise instance method.
########################################################################
require 'test/unit'

class TC_Thread_Raise_InstanceMethod < Test::Unit::TestCase
   def setup
      @thread = Thread.new{ sleep }
   end

   def test_raise_basic
      assert_respond_to(@thread, :raise)
   end

   def test_raise
      Thread.pass until @thread.status == 'sleep'
      assert_nothing_raised{ @thread.raise }
      assert_raise(RuntimeError){ @thread.join }
   end

   def test_raise_with_message
      Thread.pass until @thread.status == 'sleep'

      assert_nothing_raised{ @thread.raise('hello') }
      assert_raise(RuntimeError){ @thread.join }
      assert_raise(RuntimeError){ @thread.join } # Duplicate intentional

      begin
         @thread.join
      rescue RuntimeError => msg
         assert_equal('hello', msg.to_s)
      end
   end

   def test_raise_with_exception
      Thread.pass until @thread.status == 'sleep'

      assert_nothing_raised{ @thread.raise(StandardError) }
      assert_raise(StandardError){ @thread.join }
      assert_raise(StandardError){ @thread.join } # Duplicate intentional

      begin
         @thread.join
      rescue StandardError => msg
         assert_equal('StandardError', msg.to_s)
      end
   end

   def test_raise_with_exception_and_message
      Thread.pass until @thread.status == 'sleep'

      assert_nothing_raised{ @thread.raise(StandardError, 'hello') }
      assert_raise(StandardError){ @thread.join }
      assert_raise(StandardError){ @thread.join } # Duplicate intentional

      begin
         @thread.join
      rescue StandardError => msg
         assert_equal('hello', msg.to_s)
      end
   end

   def test_raise_with_exception_and_message_and_array
      Thread.pass until @thread.status == 'sleep'

      assert_nothing_raised{ @thread.raise(NameError, 'hello', %/foo bar/) }
      assert_raise(NameError){ @thread.join }
      assert_raise(NameError){ @thread.join } # Duplicate intentional

      begin
         @thread.join
      rescue NameError => msg
         assert_equal('hello', msg.to_s)
         assert_equal('foo bar', msg.backtrace[0])
      end
   end

   def test_raise_expected_errors
      assert_raise(ArgumentError){ @thread.raise(1,2,3,4) }
   end

   def teardown
      @thread.exit
      @thread = nil
   end
end
