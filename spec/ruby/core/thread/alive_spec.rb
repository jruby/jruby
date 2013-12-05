require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Thread#alive?" do
  it "can check it's own status" do
    ThreadSpecs.status_of_current_thread.alive?.should == true
  end

  it "describes a running thread" do
    ThreadSpecs.status_of_running_thread.alive?.should == true
  end

  it "describes a sleeping thread" do
    ThreadSpecs.status_of_sleeping_thread.alive?.should == true
  end

  it "describes a blocked thread" do
    ThreadSpecs.status_of_blocked_thread.alive?.should == true
  end

  it "describes a completed thread" do
    ThreadSpecs.status_of_completed_thread.alive?.should == false
  end

  it "describes a killed thread" do
    ThreadSpecs.status_of_killed_thread.alive?.should == false
  end

  it "describes a thread with an uncaught exception" do
    ThreadSpecs.status_of_thread_with_uncaught_exception.alive?.should == false
  end

  it "describes a dying running thread" do
    ThreadSpecs.status_of_dying_running_thread.alive?.should == true
  end

  it "describes a dying sleeping thread" do
    ThreadSpecs.status_of_dying_sleeping_thread.alive?.should == true
  end

  ruby_version_is '1.9' do
    it "return true for a killed but still running thread" do
      exit = false
      t = Thread.new do
        begin
          sleep
        ensure
          Thread.pass until exit # Ruby 1.8 won't switch threads here
        end
      end

      ThreadSpecs.spin_until_sleeping(t)

      t.kill
      t.alive?.should == true
      exit = true
      t.join
    end
  end
end
