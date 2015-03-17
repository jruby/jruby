require 'spec_helper'
require 'mspec/guards'

describe Object, "#with_tty" do
  before :each do
    ScratchPad.clear

    @guard = TTYGuard.new
    TTYGuard.stub(:new).and_return(@guard)
  end

  it "yields if STDOUT is a TTY" do
    STDOUT.should_receive(:tty?).and_return(true)
    with_tty { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "does not yield if STDOUT is not a TTY" do
    STDOUT.should_receive(:tty?).and_return(false)
    with_tty { ScratchPad.record :yield }
    ScratchPad.recorded.should_not == :yield
  end

  it "sets the name of the guard to :with_tty" do
    with_tty { }
    @guard.name.should == :with_tty
  end

  it "calls #unregister even when an exception is raised in the guard block" do
    @guard.should_receive(:match?).and_return(true)
    @guard.should_receive(:unregister)
    lambda do
      with_tty { raise Exception }
    end.should raise_error(Exception)
  end
end
