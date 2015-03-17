require 'spec_helper'
require 'mspec/guards'

describe Object, "#unspecified" do
  before :each do
    ScratchPad.clear

    @guard = UnspecifiedGuard.new
    UnspecifiedGuard.stub(:new).and_return(@guard)
  end

  it "does not yield if #standard? returns true" do
    @guard.should_receive(:standard?).and_return(true)
    unspecified { ScratchPad.record :yield }
    ScratchPad.recorded.should be_nil
  end

  it "yields if #standard? returns false" do
    @guard.should_receive(:standard?).and_return(false)
    unspecified { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "sets the name of the guard to :unspecified" do
    @guard.should_receive(:standard?).and_return(true)
    unspecified { }
    @guard.name.should == :unspecified
  end

  it "calls #unregister even when an exception is raised in the guard block" do
    guard = UnspecifiedGuard.new :rubinius
    UnspecifiedGuard.stub(:new).and_return(guard)
    guard.should_receive(:match?).and_return(true)
    guard.should_receive(:unregister)

    lambda do
      unspecified { raise Exception }
    end.should raise_error(Exception)
  end
end

describe Object, "#specified_on" do
  before :each do
    ScratchPad.clear
  end

  it "raises an Exception when passed :ruby" do
    lambda {
      specifed_on(:ruby) { ScratchPad.record :yield }
    }.should raise_error(Exception)
    ScratchPad.recorded.should_not == :yield
  end

  it "does not yield when #standard? returns true" do
    guard = SpecifiedOnGuard.new
    SpecifiedOnGuard.stub(:new).and_return(guard)
    guard.should_receive(:standard?).and_return(true)

    specified_on(:rubinius) { ScratchPad.record :yield }
    ScratchPad.recorded.should be_nil
  end

  it "does not yield when #standard? returns false and #implementation? returns false" do
    guard = SpecifiedOnGuard.new :rubinius
    SpecifiedOnGuard.stub(:new).and_return(guard)
    guard.should_receive(:standard?).and_return(false)
    guard.should_receive(:implementation?).with(:rubinius).and_return(false)

    specified_on(:rubinius) { ScratchPad.record :yield }
    ScratchPad.recorded.should be_nil
  end

  it "yields when #standard? returns false and #implementation? returns true" do
    guard = SpecifiedOnGuard.new :rubinius
    SpecifiedOnGuard.stub(:new).and_return(guard)
    guard.should_receive(:standard?).and_return(false)
    guard.should_receive(:implementation?).with(:rubinius).and_return(true)

    specified_on(:rubinius) { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "sets the name of the guard to :specified_on" do
    guard = SpecifiedOnGuard.new :rubinius
    SpecifiedOnGuard.stub(:new).and_return(guard)
    guard.should_receive(:match?).and_return(false)

    specified_on(:rubinius) { }
    guard.name.should == :specified_on
  end

  it "calls #unregister even when an exception is raised in the guard block" do
    guard = SpecifiedOnGuard.new :rubinius
    SpecifiedOnGuard.stub(:new).and_return(guard)
    guard.should_receive(:match?).and_return(true)
    guard.should_receive(:unregister)

    lambda do
      specified_on(:rubinius) { raise Exception }
    end.should raise_error(Exception)
  end
end
