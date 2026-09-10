require_relative '../../spec_helper'
require_relative 'shared/resume'

describe "Fiber#transfer" do
  it_behaves_like :fiber_resume, :transfer
end

describe "Fiber#transfer" do
  it "transfers control from one Fiber to another when called from a Fiber" do
    fiber1 = Fiber.new { :fiber1 }
    fiber2 = Fiber.new { fiber1.transfer; :fiber2 }
    fiber2.resume.should == :fiber2
  end

  it "returns to the root Fiber when finished" do
    f1 = Fiber.new { :fiber_1 }
    f2 = Fiber.new { f1.transfer; :fiber_2 }

    f2.transfer.should == :fiber_1
    f2.transfer.should == :fiber_2
  end

  it "returns control to the transferring fiber when the target finishes" do
    states = []
    runner = Fiber.new do
      Fiber.new { states << :target_end }.transfer
      states << :runner_resumed
    end

    runner.resume
    states.should == [:target_end, :runner_resumed]
  end

  it "returns control to a transferring fiber which was itself entered by Fiber#raise" do
    states = []
    target = Fiber.new do
      begin
        Fiber.yield
      rescue RuntimeError
        Fiber.new { states << :inner_end }.transfer
        states << :target_after_transfer
      end
    end

    target.resume
    target.raise "raised"
    states.should == [:inner_end, :target_after_transfer]
  end

  it "returns control according to the resume chain, not to the Fiber which transferred" do
    states = []
    outer = Fiber.new do
      middle = Fiber.new do
        inner = Fiber.new do
          Fiber.new { states << :leaf_end }.transfer
          states << :inner_after_transfer
        end
        inner.resume
        states << :middle_after_resume
      end
      middle.transfer
      states << :outer_after_transfer
    end

    outer.resume
    # The leaf has no resumer, so control follows resumes down from the root Fiber and
    # stops at outer. inner, which transferred to the leaf, stays suspended.
    states.should == [:leaf_end, :outer_after_transfer]
  end

  it "can be invoked from the same Fiber it transfers control to" do
    states = []
    fiber = Fiber.new { states << :start; fiber.transfer; states << :end }
    fiber.transfer
    states.should == [:start, :end]

    states = []
    fiber = Fiber.new { states << :start; fiber.transfer; states << :end }
    fiber.resume
    states.should == [:start, :end]
  end

  it "can not transfer control to a Fiber that has suspended by Fiber.yield" do
    states = []
    fiber1 = Fiber.new { states << :fiber1 }
    fiber2 = Fiber.new { states << :fiber2_start; Fiber.yield fiber1.transfer; states << :fiber2_end}
    fiber2.resume.should == [:fiber2_start, :fiber1]
    -> { fiber2.transfer }.should.raise(FiberError)
  end

  it "raises a FiberError when transferring to a Fiber which resumes itself" do
    fiber = Fiber.new { fiber.resume }
    -> { fiber.transfer }.should.raise(FiberError)
  end

  it "works if Fibers in different Threads each transfer to a Fiber in the same Thread" do
    # This catches a bug where Fibers are running on a thread-pool
    # and Fibers from a different Ruby Thread reuse the same native thread.
    # Caching the Ruby Thread based on the native thread is not correct in that case,
    # and the check for "fiber called across threads" in Fiber#transfer
    # might be incorrect based on that.
    2.times do
      Thread.new do
        io_fiber = Fiber.new do |calling_fiber|
          calling_fiber.transfer
        end
        io_fiber.transfer(Fiber.current)
        value = Object.new
        io_fiber.transfer(value).should.equal? value
      end.join
    end
  end

  it "transfers control between a non-main thread's root fiber to a child fiber and back again" do
    states = []
    thread = Thread.new do
      f1 = Fiber.new do |f0|
        states << 0
        value2 = f0.transfer(1)
        states << value2
        3
      end

      value1 = f1.transfer(Fiber.current)
      states << value1
      value3 = f1.transfer(2)
      states << value3
    end
    thread.join
    states.should == [0, 1, 2, 3]
  end
end
