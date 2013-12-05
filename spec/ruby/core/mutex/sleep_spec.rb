require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "Mutex#sleep" do
    it "raises ThreadError if not locked by the current thread" do
      m = Mutex.new
      lambda { m.sleep }.should raise_error(ThreadError)
    end

    it "pauses execution for approximately the duration requested" do
      m = Mutex.new
      m.lock
      duration = 0.1
      start = Time.now
      m.sleep duration
      (Time.now - start).should be_close(duration, 0.1)
    end

    it "unlocks the mutex while sleeping" do
      m = Mutex.new
      locked = false
      th = Thread.new { m.lock; locked = true; m.sleep }
      Thread.pass until locked
      Thread.pass while th.status and th.status != "sleep"
      m.locked?.should be_false
      th.run
      th.join
    end

    it "relocks the mutex when woken" do
      m = Mutex.new
      m.lock
      m.sleep(0.01)
      m.locked?.should be_true
    end

    it "relocks the mutex when woken by an exception being raised" do
      m = Mutex.new
      th = Thread.new do
        m.lock
        begin
          m.sleep
        rescue Exception
          m.locked?
        end
      end
      Thread.pass while th.status and th.status != "sleep"
      th.raise(Exception)
      th.value.should be_true
    end

    it "returns the rounded number of seconds asleep" do
      m = Mutex.new
      m.lock
      m.sleep(0.01).should be_kind_of(Integer)
    end
  end
end
