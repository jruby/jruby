require File.dirname(__FILE__) + '/../../spec_helper'
require 'mspec/runner/actions/gdb'
require 'mspec/runner/mspec'
require 'mspec/runner/example'

describe GdbAction do
  before :each do
    MSpec.stub!(:read_tags).and_return([])
  end

  it "creates an MatchFilter with its tag and desc arguments" do
    filter = mock('action filter').as_null_object
    MatchFilter.should_receive(:new).with(nil, "some", "thing").and_return(filter)
    GdbAction.new ["tag", "key"], ["some", "thing"]
  end
end

describe GdbAction, "#before" do
  before :each do
    MSpec.stub!(:read_tags).and_return([])
    @state = ExampleState.new ContextState.new("Catch#me"), "if you can"
  end

  it "does not invoke the debugger if the description does not match" do
    Kernel.should_not_receive(:yield_gdb)
    action = GdbAction.new nil, "match"
    action.before @state
  end

  it "invokes the debugger if the description matches" do
    Kernel.should_receive(:yield_gdb).with(true)
    action = GdbAction.new nil, "can"
    action.before @state
  end
end

describe GdbAction, "#register" do
  before :each do
    MSpec.stub!(:read_tags).and_return([])
    MSpec.stub!(:register)
    @action = GdbAction.new nil, nil
  end

  it "registers itself with MSpec for the :before action" do
    MSpec.should_receive(:register).with(:before, @action)
    @action.register
  end
end

describe GdbAction, "#unregister" do
  before :each do
    MSpec.stub!(:read_tags).and_return([])
    MSpec.stub!(:unregister)
    @action = GdbAction.new nil, nil
  end

  it "unregisters itself with MSpec for the :before action" do
    MSpec.should_receive(:unregister).with(:before, @action)
    @action.unregister
  end
end
