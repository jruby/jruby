require 'spec_helper'
require 'mspec/guards'

describe Object, "#as_user" do
  before :each do
    @guard = UserGuard.new
    UserGuard.stub(:new).and_return(@guard)
    ScratchPad.clear
  end

  it "yields when the Process.euid is not 0" do
    Process.stub(:euid).and_return(501)
    as_user { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "does not yield when the Process.euid is 0" do
    Process.stub(:euid).and_return(0)
    as_user { ScratchPad.record :yield }
    ScratchPad.recorded.should_not == :yield
  end

  it "sets the name of the guard to :as_user" do
    as_user { }
    @guard.name.should == :as_user
  end

  it "calls #unregister even when an exception is raised in the guard block" do
    @guard.should_receive(:match?).and_return(true)
    @guard.should_receive(:unregister)
    lambda do
      as_user { raise Exception }
    end.should raise_error(Exception)
  end
end
