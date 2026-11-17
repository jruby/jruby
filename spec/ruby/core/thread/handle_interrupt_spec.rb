require_relative '../../spec_helper'

describe "Thread.handle_interrupt" do
  def make_handle_interrupt_thread(interrupt_config, blocking = true)
    interrupt_class = Class.new(RuntimeError)

    ScratchPad.record []

    in_handle_interrupt = Queue.new
    can_continue = Queue.new

    thread = Thread.new do
      begin
        Thread.handle_interrupt(interrupt_config) do
          begin
            in_handle_interrupt << true
            if blocking
              Thread.pass # Make it clearer the other thread needs to wait for this one to be in #pop
              can_continue.pop
            else
              begin
                can_continue.pop(true)
              rescue ThreadError
                Thread.pass
                retry
              end
            end
          rescue interrupt_class
            ScratchPad << :interrupted
          end
        end
      rescue interrupt_class
        ScratchPad << :deferred
      end
    end

    in_handle_interrupt.pop
    if blocking
      # Ensure the thread is inside Thread#pop, as if thread.raise is done before it would be deferred
      Thread.pass until thread.stop?
    end
    thread.raise interrupt_class, "interrupt"
    can_continue << true
    thread.join

    ScratchPad.recorded
  end

  before :each do
    Thread.pending_interrupt?.should == false # sanity check
  end

  it "with :never defers interrupts until exiting the handle_interrupt block" do
    make_handle_interrupt_thread(RuntimeError => :never).should == [:deferred]
  end

  it "with :on_blocking defers interrupts until the next blocking call" do
    make_handle_interrupt_thread(RuntimeError => :on_blocking).should == [:interrupted]
    make_handle_interrupt_thread({ RuntimeError => :on_blocking }, false).should == [:deferred]
  end

  it "with :immediate handles interrupts immediately" do
    make_handle_interrupt_thread(RuntimeError => :immediate).should == [:interrupted]
  end

  it "with :immediate immediately runs pending interrupts, before the block" do
    Thread.handle_interrupt(RuntimeError => :never) do
      current = Thread.current
      Thread.new {
        current.raise "interrupt immediate"
      }.join

      Thread.pending_interrupt?.should == true
      -> {
        Thread.handle_interrupt(RuntimeError => :immediate) {
          flunk "not reached"
        }
      }.should raise_error(RuntimeError, "interrupt immediate")
      Thread.pending_interrupt?.should == false
    end
  end

  it "also works with suspended Fibers and does not duplicate interrupts" do
    fiber = Fiber.new { Fiber.yield }
    fiber.resume

    Thread.handle_interrupt(RuntimeError => :never) do
      current = Thread.current
      Thread.new {
        current.raise "interrupt with fibers"
      }.join

      Thread.pending_interrupt?.should == true
      -> {
        Thread.handle_interrupt(RuntimeError => :immediate) {
          flunk "not reached"
        }
      }.should raise_error(RuntimeError, "interrupt with fibers")
      Thread.pending_interrupt?.should == false
    end

    fiber.resume
  end

  it "defers an interrupt raised while the thread waits in Queue#pop with a timeout" do
    queue = Queue.new
    log = []
    thread = Thread.new do
      Thread.handle_interrupt(RuntimeError => :never) do
        log << queue.pop(timeout: 10)
      end
      log << :not_reached
    end
    thread.report_on_exception = false

    Thread.pass until thread.stop?
    thread.raise "interrupt in pop"
    thread.join(0.1).should == nil
    queue << :popped

    -> { thread.join }.should raise_error(RuntimeError, "interrupt in pop")
    log.should == [:popped]
  end

  it "defers an interrupt raised while the thread waits in SizedQueue#push" do
    queue = SizedQueue.new(1)
    queue << :first
    log = []
    thread = Thread.new do
      Thread.handle_interrupt(RuntimeError => :never) do
        log << queue.push(:second)
      end
      log << :not_reached
    end
    thread.report_on_exception = false

    Thread.pass until thread.stop?
    thread.raise "interrupt in push"
    thread.join(0.1).should == nil
    queue.pop

    -> { thread.join }.should raise_error(RuntimeError, "interrupt in push")
    log.should == [queue]
    queue.pop.should == :second
  end

  it "defers an interrupt raised while the thread waits in SizedQueue#push with a timeout" do
    queue = SizedQueue.new(1)
    queue << :first
    log = []
    thread = Thread.new do
      Thread.handle_interrupt(RuntimeError => :never) do
        log << queue.push(:second, timeout: 10)
      end
      log << :not_reached
    end
    thread.report_on_exception = false

    Thread.pass until thread.stop?
    thread.raise "interrupt in push"
    thread.join(0.1).should == nil
    queue.pop

    -> { thread.join }.should raise_error(RuntimeError, "interrupt in push")
    log.should == [queue]
    queue.pop.should == :second
  end

  it "defers an interrupt raised while the thread sleeps with a timeout" do
    log = []
    thread = Thread.new do
      Thread.handle_interrupt(RuntimeError => :never) do
        sleep 10
        log << :slept
      end
      log << :not_reached
    end
    thread.report_on_exception = false

    Thread.pass until thread.stop?
    thread.raise "interrupt in sleep"
    thread.join(0.1).should == nil
    thread.wakeup

    -> { thread.join }.should raise_error(RuntimeError, "interrupt in sleep")
    log.should == [:slept]
  end

  it "defers Thread#kill while the thread sleeps without a timeout" do
    log = []
    thread = Thread.new do
      Thread.handle_interrupt(Object => :never) do
        sleep
        log << :slept
      end
      log << :not_reached
    end

    Thread.pass until thread.stop?
    thread.kill
    thread.join(0.1).should == nil
    thread.wakeup

    thread.join.should == thread
    log.should == [:slept]
  end

  it "runs pending interrupts at the end of the block, even if there was an exception raised in the block" do
    executed = false
    -> {
      Thread.handle_interrupt(RuntimeError => :never) do
        current = Thread.current
        Thread.new {
          current.raise "interrupt exception"
        }.join

        Thread.pending_interrupt?.should == true
        executed = true
        raise "regular exception"
      end
    }.should raise_error(RuntimeError, "interrupt exception")
    executed.should == true
  end

  it "supports multiple pairs in the Hash" do
    make_handle_interrupt_thread(ArgumentError => :never, RuntimeError => :never).should == [:deferred]
  end
end
