require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is "" ... "1.9" do
  describe "Thread.critical=" do
    before(:each) do
      ScratchPad.clear
    end

    it "is a persistent attribute" do
      Thread.critical = true
      Thread.critical.should == true
      Thread.critical = false
      ThreadSpecs.critical_is_reset.should == true
    end

    it "allows all non-bool arguments" do
      Thread.critical = "Hello"
      Thread.critical.should == true

      Thread.critical = nil
      ThreadSpecs.critical_is_reset.should == true
    end

    it "functions as a critical section" do
      ThreadSpecs.counter = 0
      iters = 50
      t = Thread.new { ThreadSpecs.increment_counter(iters) }
      ThreadSpecs.increment_counter(iters)
      t.join
      ThreadSpecs.counter.should == iters * 2
    end

    it "does not change status of other existing threads" do
      t = ThreadSpecs.create_critical_thread { ScratchPad.record Thread.main.status }
      Thread.pass while t.status and t.status != false
      ScratchPad.recorded.should == "run"
    end

    it "is reentrant" do
      Thread.critical = true
      Thread.critical = true
      Thread.critical.should == true
      Thread.critical = false
      Thread.critical = false
      ThreadSpecs.critical_is_reset.should == true
    end

    it "can be mismatched" do
      Thread.critical = true
      Thread.critical = true
      Thread.critical.should == true
      Thread.critical = false
      ThreadSpecs.critical_is_reset.should == true
    end

    # Hangs on 1.8.6.114 OS X, possibly also on Linux
    quarantine! do
    it "schedules other threads on Thread.pass" do
      ThreadSpecs.critical_thread_yields_to_main_thread { Thread.pass }
    end

    it "schedules other threads on sleep" do
      ThreadSpecs.critical_thread_yields_to_main_thread(true) { sleep }
    end
    end

    it "schedules other threads on Thread.stop" do
      # Note that Thread.Stop resets Thread.critical, whereas sleep does not
      ThreadSpecs.critical_thread_yields_to_main_thread(false, true) { Thread.stop }
    end

    it "defers exit" do
      critical_thread = ThreadSpecs.create_and_kill_critical_thread()
      Thread.pass while critical_thread.status
      ScratchPad.recorded.should == "status=aborting"
    end

    it "defers exit until Thread.pass" do
      critical_thread = ThreadSpecs.create_and_kill_critical_thread(true)
      Thread.pass while critical_thread.status
      ScratchPad.recorded.should == nil
    end

    not_compliant_on(:ironruby) do # requires green threads so that another thread can be scheduled when the critical thread is killed
      it "is not reset if the critical thread is killed" do
        critical_thread = ThreadSpecs.create_and_kill_critical_thread(true)
        Thread.pass while critical_thread.status
        Thread.critical.should == true

        Thread.critical = false
        ThreadSpecs.critical_is_reset.should == true
      end
    end
  end
end
