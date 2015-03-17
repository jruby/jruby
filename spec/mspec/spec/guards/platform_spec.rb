require 'spec_helper'
require 'mspec/guards'

describe Object, "#platform_is" do
  before :each do
    @guard = PlatformGuard.new :dummy
    PlatformGuard.stub(:new).and_return(@guard)
    ScratchPad.clear
  end

  it "does not yield when #platform? returns false" do
    @guard.stub(:platform?).and_return(false)
    platform_is(:ruby) { ScratchPad.record :yield }
    ScratchPad.recorded.should_not == :yield
  end

  it "yields when #platform? returns true" do
    @guard.stub(:platform?).and_return(true)
    platform_is(:solarce) { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "sets the name of the guard to :platform_is" do
    platform_is(:solarce) { }
    @guard.name.should == :platform_is
  end

  it "calls #unregister even when an exception is raised in the guard block" do
    @guard.should_receive(:match?).and_return(true)
    @guard.should_receive(:unregister)
    lambda do
      platform_is(:solarce) { raise Exception }
    end.should raise_error(Exception)
  end
end

describe Object, "#platform_is_not" do
  before :each do
    @guard = PlatformGuard.new :dummy
    PlatformGuard.stub(:new).and_return(@guard)
    ScratchPad.clear
  end

  it "does not yield when #platform? returns true" do
    @guard.stub(:platform?).and_return(true)
    platform_is_not(:ruby) { ScratchPad.record :yield }
    ScratchPad.recorded.should_not == :yield
  end

  it "yields when #platform? returns false" do
    @guard.stub(:platform?).and_return(false)
    platform_is_not(:solarce) { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "sets the name of the guard to :platform_is_not" do
    platform_is_not(:solarce) { }
    @guard.name.should == :platform_is_not
  end

  it "calls #unregister even when an exception is raised in the guard block" do
    @guard.should_receive(:match?).and_return(false)
    @guard.should_receive(:unregister)
    lambda do
      platform_is_not(:solarce) { raise Exception }
    end.should raise_error(Exception)
  end
end

describe Object, "#platform_is :wordsize => SIZE_SPEC" do
  before :each do
    @guard = PlatformGuard.new :darwin, :wordsize => 32
    @guard.stub(:platform?).and_return(true)
    PlatformGuard.stub(:new).and_return(@guard)
    ScratchPad.clear
  end

  it "yields when #wordsize? returns true" do
    @guard.stub(:wordsize?).and_return(true)
    platform_is(:wordsize => 32) { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "doesn not yield when #wordsize? returns false" do
    @guard.stub(:wordsize?).and_return(false)
    platform_is(:wordsize => 32) { ScratchPad.record :yield }
    ScratchPad.recorded.should_not == :yield
  end
end

describe Object, "#platform_is_not :wordsize => SIZE_SPEC" do
  before :each do
    @guard = PlatformGuard.new :darwin, :wordsize => 32
    @guard.stub(:platform?).and_return(true)
    PlatformGuard.stub(:new).and_return(@guard)
    ScratchPad.clear
  end

  it "yields when #wordsize? returns false" do
    @guard.stub(:wordsize?).and_return(false)
    platform_is_not(:wordsize => 32) { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "doesn not yield when #wordsize? returns true" do
    @guard.stub(:wordsize?).and_return(true)
    platform_is_not(:wordsize => 32) { ScratchPad.record :yield }
    ScratchPad.recorded.should_not == :yield
  end
end
