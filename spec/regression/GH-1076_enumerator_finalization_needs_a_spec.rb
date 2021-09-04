# We must ensure enumerators finalize properly and do not leave a lot of garbage
# around nor leave threads running. This test attempts to brutalize the enum
# subsystem and the JVM GC in order to prove that enumerators are getting cleaned
# up. There may be a better way to do this.

require 'rspec'

describe "An Enumerator that has been abandoned" do
  it "cleans itself up properly" do
    begin
      thread_bean = java.lang.management.ManagementFactory.thread_mx_bean
      max_threads = JRuby.runtime.fiber_executor.maximum_pool_size

      # Thread count before enumerators
      thread_count = thread_bean.thread_count

      # Attempt to create and abandon 10000 enumerators. This should blow up quickly
      # if they are not cleaning up their threads properly.
      100.times do
        100.times do
          Enumerator.new {|y| y.yield(1); y.yield(2) }.next
        end
        JRuby.gc
      end

      # Allow GC and other threads plenty of time to clean up
      10.times do
        JRuby.gc
      end

      # Try to force finalizers to run
      java.lang.Runtime.runtime.run_finalization

      # Spin for a while, hoping to make this pass
      1000.times do
        JRuby.gc
        JRuby.runtime.fiber_executor.maximum_pool_size = 1
        break if (thread_bean.thread_count - thread_count) < 10
        Thread.pass
      end

      # Final thread count should be within ~10 threads of original (allowing for
      # JVM GC, finalizer, reference queue, etc threads to have spun up).
      expect(thread_bean.thread_count - thread_count).to be < 10
    ensure
      JRuby.runtime.fiber_executor.maximum_pool_size = max_threads
    end
  end
end