require 'test/unit'
require 'jruby/vm'

class TestJRubyVM < Test::Unit::TestCase
  MESSAGE_TIMEOUT = 5
  THREAD_JOIN_TIMEOUT_MS = 5_000

  def setup
    drain_messages
  end

  def teardown
    drain_messages
  end

  def test_send_message_accepts_string_and_integer
    ['message', 1].each do |message|
      JRuby::VM.send_message(JRuby::VM_ID, message)

      assert_equal message, wait_for_message
    end
  end

  def test_child_vm_accepts_parent_vm_id
    code = <<~'RUBY'
      require 'jruby/vm'

      parent = JRuby::VM.get_message
      JRuby::VM.send_message(parent, 'ok')
    RUBY

    vm = JRuby::VM.spawn('--disable=rubyopt', '-e', code)
    vm.start
    sent = false

    begin
      vm << JRuby::VM_ID
      sent = true

      assert_equal 'ok', wait_for_message
    ensure
      # Unblock the child when the integer send fails, as it does before the fix.
      vm.queue.put(JRuby::VM_ID) unless sent
      vm.main.join(THREAD_JOIN_TIMEOUT_MS)
      assert_equal false, vm.main.alive?, 'child JRuby VM did not terminate'
    end
  end

  private

  def wait_for_message
    deadline = Process.clock_gettime(Process::CLOCK_MONOTONIC) + MESSAGE_TIMEOUT

    loop do
      message = JRuby::VM.poll_message
      return message if message

      now = Process.clock_gettime(Process::CLOCK_MONOTONIC)
      flunk 'timed out waiting for JRuby::VM message' if now >= deadline

      sleep 0.01
    end
  end

  def drain_messages
    loop do
      break unless JRuby::VM.poll_message
    end
  end
end
