# We must ensure enumerators finalize properly and do not leave a lot of garbage
# around nor leave threads running. This test attempts to brutalize the enum
# subsystem and the JVM GC in order to prove that enumerators are getting cleaned
# up. There may be a better way to do this.

require 'rspec'

describe "An Enumerator that has been abandoned" do
  it "cleans itself up properly" do
    thread_bean = java.lang.management.ManagementFactory.thread_mx_bean
    
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
      break if (thread_bean.thread_count - thread_count) < 200
      Thread.pass
    end
    
    # Final thread count should be within ~200 threads of original. Because
    # Enumerator threads use a thread pool, proper cleanup of the above loop
    # will leave around 100 threads alive in that pool for each iteration. These
    # threads are just waiting for more work, and will age out on their own...
    # but waiting for that here would take too long. Using 200 is a safe
    # threshold, since if enumerators are not being cleaned up we should have
    # thousands of extra threads here.
    (thread_bean.thread_count - thread_count).should < 200
  end
end if RUBY_VERSION >= "1.9"