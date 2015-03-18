require 'spec_helper'
require 'mspec/guards'

describe Object, "#process_is_foreground" do
  before :each do
    MSpec.clear_modes
    ScratchPad.clear

    @guard = BackgroundGuard.new
    BackgroundGuard.stub(:new).and_return(@guard)
  end

  it "yields if MSpec.mode?(:background) is false" do
    MSpec.mode?(:background).should be_false
    process_is_foreground { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "does not yield if MSpec.mode?(:background) is true" do
    MSpec.register_mode :background
    process_is_foreground { ScratchPad.record :yield }
    ScratchPad.recorded.should_not == :yield
  end

  it "sets the name of the guard to :process_is_foreground" do
    process_is_foreground { ScratchPad.record :yield }
    @guard.name.should == :process_is_foreground
  end

  it "calls #unregister even when an exception is raised in the guard block" do
    @guard.should_receive(:unregister)
    lambda do
      process_is_foreground { raise Exception }
    end.should raise_error(Exception)
  end
end
