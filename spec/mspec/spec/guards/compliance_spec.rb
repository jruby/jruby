require 'spec_helper'
require 'mspec/guards'

describe Object, "#compliant_on" do
  before :all do
    @verbose = $VERBOSE
    $VERBOSE = nil
    @ruby_name = Object.const_get :RUBY_NAME if Object.const_defined? :RUBY_NAME
  end

  after :all do
    $VERBOSE = @verbose
    if @ruby_name
      Object.const_set :RUBY_NAME, @ruby_name
    else
      Object.send :remove_const, :RUBY_NAME
    end
  end

  before :each do
    ScratchPad.clear
  end

  it "raises an Exception when passed :ruby" do
    Object.const_set :RUBY_NAME, "jruby"
    lambda {
      compliant_on(:ruby) { ScratchPad.record :yield }
    }.should raise_error(Exception)
    ScratchPad.recorded.should_not == :yield
  end

  it "does not yield when #standard? and #implementation? return false" do
    Object.const_set :RUBY_NAME, "jruby"
    compliant_on(:rubinius, :ironruby) { ScratchPad.record :yield }
    ScratchPad.recorded.should_not == :yield
  end

  it "yields when #standard? returns true" do
    Object.const_set :RUBY_NAME, "ruby"
    compliant_on(:rubinius) { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "yields when #implementation? returns true" do
    Object.const_set :RUBY_NAME, "jruby"
    compliant_on(:jruby) { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "does not yield when #implementation? returns false" do
    Object.const_set :RUBY_NAME, "jruby"
    compliant_on(:rubinius) { ScratchPad.record :yield }
    ScratchPad.recorded.should_not == :yield
  end
end

describe Object, "#compliant_on" do
  before :each do
    @guard = CompliantOnGuard.new :any
    CompliantOnGuard.stub(:new).and_return(@guard)
  end

  it "sets the name of the guard to :compliant_on" do
    compliant_on(:rubinius) { }
    @guard.name.should == :compliant_on
  end

  it "calls #unregister even when an exception is raised in the guard block" do
    @guard.should_receive(:unregister)
    lambda do
      compliant_on(:rubinius) { raise Exception }
    end.should raise_error(Exception)
  end
end

describe Object, "#not_compliant_on" do
  before :all do
    @verbose = $VERBOSE
    $VERBOSE = nil
    @ruby_name = Object.const_get :RUBY_NAME if Object.const_defined? :RUBY_NAME
  end

  after :all do
    $VERBOSE = @verbose
    if @ruby_name
      Object.const_set :RUBY_NAME, @ruby_name
    else
      Object.send :remove_const, :RUBY_NAME
    end
  end

  before :each do
    ScratchPad.clear
  end

  it "raises an Exception when passed :ruby" do
    Object.const_set :RUBY_NAME, "jruby"
    lambda {
      not_compliant_on(:ruby) { ScratchPad.record :yield }
    }.should raise_error(Exception)
    ScratchPad.recorded.should_not == :yield
  end

  it "yields when #standard? returns true" do
    Object.const_set :RUBY_NAME, "ruby"
    not_compliant_on(:rubinius) { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "does not yield when #implementation? returns true" do
    Object.const_set :RUBY_NAME, "jruby "
    not_compliant_on(:jruby) { ScratchPad.record :yield }
    ScratchPad.recorded.should_not == :yield
  end

  it "yields when #implementation? returns false" do
    Object.const_set :RUBY_NAME, "jruby "
    not_compliant_on(:rubinius) { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end
end

describe Object, "#not_compliant_on" do
  before :each do
    @guard = NotCompliantOnGuard.new :any
    NotCompliantOnGuard.stub(:new).and_return(@guard)
  end

  it "sets the name of the guard to :not_compliant_on" do
    not_compliant_on(:rubinius) { }
    @guard.name.should == :not_compliant_on
  end

  it "calls #unregister even when an exception is raised in the guard block" do
    @guard.should_receive(:unregister)
    lambda do
      not_compliant_on(:rubinius) { raise Exception }
    end.should raise_error(Exception)
  end
end
