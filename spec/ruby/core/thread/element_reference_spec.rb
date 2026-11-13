require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe "Thread#[]" do
  it "gives access to thread local values" do
    th = Thread.new do
      Thread.current[:value] = 5
    end
    th.join
    th[:value].should == 5
    Thread.current[:value].should == nil
  end

  it "is not shared across threads" do
    t1 = Thread.new do
      Thread.current[:value] = 1
    end
    t2 = Thread.new do
      Thread.current[:value] = 2
    end
    [t1,t2].each {|x| x.join}
    t1[:value].should == 1
    t2[:value].should == 2
  end

  it "is accessible using strings or symbols" do
    t1 = Thread.new do
      Thread.current[:value] = 1
    end
    t2 = Thread.new do
      Thread.current["value"] = 2
    end
    [t1,t2].each {|x| x.join}
    t1[:value].should == 1
    t1["value"].should == 1
    t2[:value].should == 2
    t2["value"].should == 2
  end

  it "converts a key that is neither String nor Symbol with #to_str" do
    key = mock('value')
    key.should_receive(:to_str).and_return('value')

    th = Thread.new do
      Thread.current[:value] = 1
    end.join

    th[key].should == 1
  end

  it "returns the local of the fiber running in the thread when called from another thread" do
    q1 = Queue.new
    q2 = Queue.new
    th = Thread.new do
      Thread.current[:value] = :thread
      Fiber.new do
        Thread.current[:value] = :fiber
        q1 << :ready
        q2.pop
      end.resume
      q1 << :ready
      q2.pop
    end

    begin
      q1.pop
      th[:value].should == :fiber
      q2 << :done
      q1.pop
      th[:value].should == :thread
    ensure
      q2 << :done << :done
      th.join
    end
  end

  it "raises exceptions on the wrong type of keys" do
    -> { Thread.current[nil] }.should.raise(TypeError)
    -> { Thread.current[5] }.should.raise(TypeError)
  end
end
