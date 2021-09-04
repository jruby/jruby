require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby/util'

describe "JRuby::Util.synchronized" do
  it "locks the Java object monitor for the given object while the block is active" do
    o = Object.new
    expect(java.lang.Thread.holds_lock(o)).to eq(false)
    JRuby::Util.synchronized(o) do
      expect(java.lang.Thread.holds_lock(o)).to eq(true)
    end
    expect(java.lang.Thread.holds_lock(o)).to eq(false)
  end

  it "uses the proxied Java object, not the proxy" do
    o = java.lang.Object.new
    o2 = java.util.Collections.singleton_list(o)[0]

    # same Java object but different wrappers
    expect(o).to eq(o2)
    expect(JRuby.reference(o)).to_not equal(JRuby.reference(o2))

    result = []
    JRuby::Util.synchronized(o) do
      Thread.new do
        JRuby::Util.synchronized(o2) do
          result << :thread
          JRuby::Util.notify(o)
          JRuby::Util.wait(o, 100_000)
          result << :thread
          JRuby::Util.notify_all(o)
        end
      end
      JRuby::Util.wait(o2)
      result << :main
      JRuby::Util.notify_all(o2)
      JRuby::Util.wait(o2, 100_000)
      result << :main
    end
    expect(result).to eq([:thread, :main, :thread, :main])
  end
end

describe "JRuby::Util.wait" do
  it "raises IllegalMonitorStateException if the object's monitor is not held" do
    o = Object.new
    expect { JRuby::Util.wait(o) }.to raise_error(java.lang.IllegalMonitorStateException)
  end

  it "yields the lock when called against a locked object" do
    result = []
    o = Object.new
    JRuby::Util.synchronized(o) do
      result << :outer_before
      Thread.new do
        JRuby::Util.synchronized(o) do
          result << :inner_before
          JRuby::Util.notify(o)
          result << :inner_after
        end
      end
      result << :outer_before_wait
      JRuby::Util.wait(o)
      result << :outer_after_wait
    end

    expect(result).to eq([:outer_before, :outer_before_wait, :inner_before, :inner_after, :outer_after_wait])
  end

  it "waits the specified time millis and then reacquires the lock" do
    o = Object.new
    JRuby::Util.synchronized(o) do
      t = Time.now
      JRuby::Util.wait(o, 500)
      expect(Time.now - t).to satisfy {|n| n >= 0.5}
    end
  end

  it "waits the specified time millis and nanos and then reacquires the lock" do
    o = Object.new
    JRuby::Util.synchronized(o) do
      t = Time.now
      100.times { JRuby::Util.wait(o, 0, 999_999) }
      expect(Time.now - t).to satisfy {|n| n >= 0.1}
    end
  end
end

describe "JRuby::Util.notify_all" do
  it "raises IllegalMonitorStateException if the object's monitor is not held" do
    o = Object.new
    expect {JRuby::Util.notify_all(o) }.to raise_error(java.lang.IllegalMonitorStateException)
  end

  it "notifies all waiters" do
    result = []
    o = Object.new
    threads = nil
    JRuby::Util.synchronized(o) do
      threads = 2.times.map {|i|
        Thread.new do
          JRuby::Util.synchronized(o) do
            result << :waiting
            JRuby::Util.wait(o)
            result << :woke
          end
        end
      }
    end
    Thread.pass until result == [:waiting, :waiting]
    JRuby::Util.synchronized(o) do
      JRuby::Util.notify_all(o)
    end
    threads.each(&:join)
    expect(result).to eq([:waiting, :waiting, :woke, :woke])
  end
end

# Other functionality tested above in wait examples
describe "JRuby::Util.notify" do
  it "raises IllegalMonitorStateException if the object's monitor is not held" do
    o = Object.new
    expect {JRuby::Util.notify(o) }.to raise_error(java.lang.IllegalMonitorStateException)
  end
end
