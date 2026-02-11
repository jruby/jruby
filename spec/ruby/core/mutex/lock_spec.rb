require_relative '../../spec_helper'

describe "Mutex#lock" do
  it "returns self" do
    m = Mutex.new
    m.lock.should == m
    m.unlock
  end

  it "blocks the caller if already locked" do
    m = Mutex.new
    m.lock
    -> { m.lock }.should block_caller
  end

  it "does not block the caller if not locked" do
    m = Mutex.new
    -> { m.lock }.should_not block_caller
  end

  # Unable to find a specific ticket but behavior change may be
  # related to this ML thread.
  it "raises a deadlock ThreadError when used recursively" do
    m = Mutex.new
    m.lock
    -> {
      m.lock
    }.should raise_error(ThreadError, /deadlock/)
  end

  it "raises a deadlock ThreadError when multiple fibers from the same thread try to lock" do
    m = Mutex.new

    m.lock
    f0 = Fiber.new do
      m.lock
    end
    -> { f0.resume }.should raise_error(ThreadError, /deadlock/)

    m.unlock
    f1 = Fiber.new do
      m.lock
      Fiber.yield
    end
    f2 = Fiber.new do
      m.lock
    end
    f1.resume
    -> { f2.resume }.should raise_error(ThreadError, /deadlock/)
  end
end
