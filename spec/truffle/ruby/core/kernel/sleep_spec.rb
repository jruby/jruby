require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#sleep" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:sleep)
  end

  it "pauses execution for approximately the duration requested" do
    duration = 0.1
    start = Time.now
    sleep duration
    (Time.now - start).should be_close(duration, 0.1)
  end

  it "returns the rounded number of seconds asleep" do
    sleep(0.01).should be_kind_of(Integer)
  end

  it "raises a TypeError when passed a non-numeric duration" do
    lambda { sleep(nil)   }.should raise_error(TypeError)
    lambda { sleep('now') }.should raise_error(TypeError)
    lambda { sleep('2')   }.should raise_error(TypeError)
  end

  it "pauses execution indefinitely if not given a duration" do
    lock = Channel.new
    t = Thread.new do
      lock << :ready
      sleep
      5
    end
    lock.receive.should == :ready
    # wait until the thread has gone to sleep
    Thread.pass while t.status and t.status != "sleep"
    t.run
    t.value.should == 5
  end
end

describe "Kernel.sleep" do
  it "needs to be reviewed for spec completeness"
end
