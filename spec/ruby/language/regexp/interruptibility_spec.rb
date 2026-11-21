require_relative '../../spec_helper'

describe "Regexp matching" do
  before :each do
    ready = Queue.new
    input = 'a' * 1000 + '!'
    @thread = Thread.new do
      ready << true
      # The backreference prevents caching from eliminating the backtracking.
      /\A(a|aa)+\1\z/.match(input)
    rescue RuntimeError => error
      error
    end

    ready.pop
    sleep 0.01 # Allow the thread to enter the regexp match.
  end

  after :each do
    @thread.kill
    @thread.join
  end

  it "keeps the thread status as run during a long match" do
    @thread.status.should == 'run'
  end

  it "can be interrupted by Thread#raise during a long match" do
    @thread.raise RuntimeError, 'interrupted'
    @thread.join(5).should == @thread
    @thread.value.should.is_a?(RuntimeError)
    @thread.value.message.should == 'interrupted'
  end

  it "can be interrupted by Thread#kill during a long match" do
    @thread.kill
    @thread.join(5).should == @thread
    @thread.status.should == false
  end
end
