require 'test/unit'
require 'java'

class TestJavaWrapperDeadlock < Test::Unit::TestCase
  import org.jruby.test.Runner

  def test_deadlock_due_to_java_object_wrapping_locking_on_java_instances
    Runner.getRunner.runJob java.lang.Runnable.impl {
      Thread.new do
        runner = Runner.getRunner
        assert runner.isRunning, "runner should be running"
      end.join
    }
  end
end
