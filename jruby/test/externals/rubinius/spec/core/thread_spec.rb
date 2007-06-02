require File.dirname(__FILE__) + '/../spec_helper'

unless defined? Channel
  require 'thread'
  class Channel < Queue
    alias receive shift
  end
end

context "For Thread" do
  specify "new should create a thread executing the given block" do
    c = Channel.new
    Thread.new { c << true }.join
    c << false
    c.receive.should == true
  end

  specify "current should return a thread" do
    current = Thread.current
    current.class.should == Thread
  end

  specify "current should return the current thread" do
    t = Thread.new { Thread.current }
    t.value.should.equal? t
  end

  specify "main should return the main thread" do
    Thread.new { @main = Thread.main ; @current = Thread.current}.join
    @main.should_not == @current
  end
end

context "For a Thread instance" do
  specify "value should return the result of the block" do
    Thread.new { 3 }.value.should == 3
  end

  specify "alive? should return true as long as the thread is alive" do
    c = Channel.new
    t = Thread.new { c.receive }
    begin
      t.alive?.should == true
    ensure
      c << nil
    end
  end

  specify "alive? should return false when the thread is finished" do
    t = Thread.new {}
    t.join
    t.alive?.should == false
  end

  specify "join should return the thread when it is finished" do
    t = Thread.new {}
    t.join.should.equal? t
  end

  specify "join with a timeout should return the thread when it is finished" do
    t = Thread.new {}
    t.join
    t.join(0).should.equal? t
  end

  specify "join with a timeout should return nil if it is not finished" do
    c = Channel.new
    t = Thread.new { c.receive }
    begin
      t.join(0).should == nil
    ensure
      c << true
    end
  end

  specify "joining should raise any exceptions encountered in the thread body" do
    t = Thread.new { raise NotImplementedError.new("Just kidding") }
    should_raise(NotImplementedError) { t.join }
  end

  specify "status should report its current state" do
    v = nil
    t = Thread.new {
      v = Thread.current.status
    }
    t.join
    v.should == 'run'
    t.status.should == false
  end

  specify "inspect should show its status" do
    x = nil
    t = Thread.new { x = Thread.current.inspect; sleep 4 }
    Thread.pass until t.status == false || t.status == 'sleep'
    x.include?('run').should == true
    t.inspect.include?('sleep').should == true
    t.kill
    Thread.pass while t.alive?
    t.inspect.include?('dead').should == true
  end
end
