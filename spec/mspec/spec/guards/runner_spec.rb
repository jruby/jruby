require 'spec_helper'
require 'mspec/guards'

describe RunnerGuard, "#match?" do
  before :all do
    @verbose = $VERBOSE
    $VERBOSE = nil
  end

  after :all do
    $VERBOSE = @verbose
  end

  it "returns true when passed :mspec and ENV['MSPEC_RUNNER'] is true" do
    ENV['MSPEC_RUNNER'] = '1'
    RunnerGuard.new(:mspec).match?.should == true
  end

  it "returns false when passed :mspec and ENV['MSPEC_RUNNER'] is false" do
    ENV.delete 'MSPEC_RUNNER'
    RunnerGuard.new(:mspec).match?.should == false
  end

  it "returns true when passed :rspec and ENV['RSPEC_RUNNER'] is false but the constant Spec exists" do
    ENV.delete 'RSPEC_RUNNER'
    Object.const_set(:Spec, 1) unless Object.const_defined?(:Spec)
    RunnerGuard.new(:rspec).match?.should == true
  end

  it "returns true when passed :rspec and ENV['RSPEC_RUNNER'] is true but the constant Spec does not exist" do
    ENV['RSPEC_RUNNER'] = '1'
    Object.stub(:const_defined?).and_return(false)
    RunnerGuard.new(:rspec).match?.should == true
  end
end

describe Object, "#runner_is" do
  before :each do
    @guard = RunnerGuard.new
    RunnerGuard.stub(:new).and_return(@guard)
    ScratchPad.clear
  end

  it "yields when #match? returns true" do
    @guard.stub(:match?).and_return(true)
    runner_is(:mspec) { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "does not yield when #match? returns false" do
    @guard.stub(:match?).and_return(false)
    runner_is(:mspec) { ScratchPad.record :yield }
    ScratchPad.recorded.should_not == :yield
  end

  it "sets the name of the guard to :runner_is" do
    runner_is(:mspec) { }
    @guard.name.should == :runner_is
  end

  it "calls #unregister even when an exception is raised in the guard block" do
    @guard.should_receive(:match?).and_return(true)
    @guard.should_receive(:unregister)
    lambda do
      runner_is(:mspec) { raise Exception }
    end.should raise_error(Exception)
  end
end

describe Object, "#runner_is_not" do
  before :each do
    @guard = RunnerGuard.new
    RunnerGuard.stub(:new).and_return(@guard)
    ScratchPad.clear
  end

  it "does not yield when #match? returns true" do
    @guard.stub(:match?).and_return(true)
    runner_is_not(:mspec) { ScratchPad.record :yield }
    ScratchPad.recorded.should_not == :yield
  end

  it "yields when #match? returns false" do
    @guard.stub(:match?).and_return(false)
    runner_is_not(:mspec) { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "sets the name of the guard to :runner_is_not" do
    runner_is_not(:mspec) { }
    @guard.name.should == :runner_is_not
  end

  it "calls #unregister even when an exception is raised in the guard block" do
    @guard.should_receive(:match?).and_return(false)
    @guard.should_receive(:unregister)
    lambda do
      runner_is_not(:mspec) { raise Exception }
    end.should raise_error(Exception)
  end
end
