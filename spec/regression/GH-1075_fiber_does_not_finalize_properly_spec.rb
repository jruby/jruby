# We must ensure fibers finalize properly and do not leave a lot of garbage
# around nor leave threads running. This test attempts to brutalize the fiber
# subsystem and the JVM GC in order to prove that fibers are getting cleaned
# up. There may be a better way to do this.

require 'rspec'

describe "A Fiber that has been abandoned" do
  it "cleans itself up properly" do
    thread_bean = java.lang.management.ManagementFactory.thread_mx_bean
    
    # Thread count before fibers
    thread_count = thread_bean.thread_count
    
    # Attempt to create and abandon 10000 fibers. This should blow up quickly
    # if they are not cleaning up their threads properly.
    100.times do
      100.times do
        Fiber.new { Fiber.yield }.resume
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
      break if (thread_bean.thread_count - thread_count) < 10
      Thread.pass
    end
    
    # Final thread count should be within ~10 threads of original (allowing for
    # JVM GC, finalizer, reference queue, etc threads to have spun up).
    (thread_bean.thread_count - thread_count).should < 10
  end
end if RUBY_VERSION >= "1.9"