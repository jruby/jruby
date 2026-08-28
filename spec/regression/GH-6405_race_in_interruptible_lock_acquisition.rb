# Acquired locks must go into the thread's lock list, so they are released when the thread terminates.
# The following code simulates threads attempting to lock at the same time another thread (main) is
# trying to kill them. This led to the regression behind #6405 and #6326 where the kill event would
# interrupt the thread before it had added the locked lock to its lock list, resulting in it remaining
# locked forever.
describe "A Thread that acquires a lock immediately before being killed" do
  it "releases that lock when killed" do
    def rand_sleep
      sleep(rand / 500)
    end

    mutex = Mutex.new
    200.times do
      ts = 3.times.map do
        Thread.new do
          rand_sleep
          mutex.synchronize { rand_sleep }
        end
      end
      3.times do
        rand_sleep
        ts.sample.kill
      end

      # lock in main thread to force some threads to wait
      mutex.synchronize {}

      ts.each(&:join)
    end

    expect(mutex).to_not be_locked
  end
end
