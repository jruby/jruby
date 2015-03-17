require 'spec_helper'
require 'mspec/guards'

describe BugGuard, "#match? when #implementation? is 'ruby'" do
  before :all do
    @verbose = $VERBOSE
    $VERBOSE = nil
  end

  after :all do
    $VERBOSE = @verbose
  end

  before :each do
    @ruby_version = Object.const_get :RUBY_VERSION
    @ruby_patch = Object.const_get :RUBY_PATCHLEVEL
    @ruby_name = Object.const_get :RUBY_NAME

    Object.const_set :RUBY_VERSION, '1.8.6'
    Object.const_set :RUBY_PATCHLEVEL, 114
    Object.const_set :RUBY_NAME, 'ruby'
  end

  after :each do
    Object.const_set :RUBY_VERSION, @ruby_version
    Object.const_set :RUBY_PATCHLEVEL, @ruby_patch
    Object.const_set :RUBY_NAME, @ruby_name
  end

  it "returns false when version argument is less than RUBY_VERSION and RUBY_PATCHLEVEL" do
    BugGuard.new("#1", "1.8.5").match?.should == false
    BugGuard.new("#1", "1.8.6.113").match?.should == false
  end

  it "returns true when version argument is equal to RUBY_VERSION and RUBY_PATCHLEVEL" do
    BugGuard.new("#1", "1.8.6.114").match?.should == true
  end

  it "returns true when version argument is greater than RUBY_VERSION and RUBY_PATCHLEVEL" do
    BugGuard.new("#1", "1.8.7").match?.should == true
    BugGuard.new("#1", "1.8.6.115").match?.should == true
  end

  it "returns true when version argument implicitly includes RUBY_VERSION and RUBY_PATCHLEVEL" do
    BugGuard.new("#1", "1.8").match?.should == true
    BugGuard.new("#1", "1.8.6").match?.should == true
  end

  it "returns false when MSpec.mode?(:no_ruby_bug) is true" do
    MSpec.should_receive(:mode?).with(:no_ruby_bug).twice.and_return(:true)
    BugGuard.new("#1", "1.8.5").match?.should == false
    BugGuard.new("#1", "1.8").match?.should == false
  end
end

describe BugGuard, "#match? when #implementation? is not 'ruby'" do
  before :all do
    @verbose = $VERBOSE
    $VERBOSE = nil
  end

  after :all do
    $VERBOSE = @verbose
  end

  before :each do
    @ruby_version = Object.const_get :RUBY_VERSION
    @ruby_patch = Object.const_get :RUBY_PATCHLEVEL
    @ruby_name = Object.const_get :RUBY_NAME

    Object.const_set :RUBY_VERSION, '1.8.6'
    Object.const_set :RUBY_PATCHLEVEL, 114
    Object.const_set :RUBY_NAME, 'jruby'
  end

  after :each do
    Object.const_set :RUBY_VERSION, @ruby_version
    Object.const_set :RUBY_PATCHLEVEL, @ruby_patch
    Object.const_set :RUBY_NAME, @ruby_name
  end

  it "returns false when version argument is less than RUBY_VERSION and RUBY_PATCHLEVEL" do
    BugGuard.new("#1", "1.8").match?.should == false
    BugGuard.new("#1", "1.8.6").match?.should == false
  end

  it "returns false when version argument is equal to RUBY_VERSION and RUBY_PATCHLEVEL" do
    BugGuard.new("#1", "1.8.6.114").match?.should == false
  end

  it "returns false when version argument is greater than RUBY_VERSION and RUBY_PATCHLEVEL" do
    BugGuard.new("#1", "1.8.7").match?.should == false
    BugGuard.new("#1", "1.8.6.115").match?.should == false
  end

  it "returns false when MSpec.mode?(:no_ruby_bug) is true" do
    MSpec.stub(:mode?).and_return(:true)
    BugGuard.new("#1", "1.8.6").match?.should == false
    BugGuard.new("#1", "1.8.6.114").match?.should == false
    BugGuard.new("#1", "1.8.6.115").match?.should == false
  end
end

describe Object, "#ruby_bug" do
  before :each do
    @guard = BugGuard.new "#1234", "x.x.x.x"
    BugGuard.stub(:new).and_return(@guard)
    ScratchPad.clear
  end

  it "yields when #match? returns false" do
    @guard.stub(:match?).and_return(false)
    ruby_bug("#1234", "1.8.6") { ScratchPad.record :yield }
    ScratchPad.recorded.should == :yield
  end

  it "does not yield when #match? returns true" do
    @guard.stub(:match?).and_return(true)
    ruby_bug("#1234", "1.8.6") { ScratchPad.record :yield }
    ScratchPad.recorded.should_not == :yield
  end

  it "requires a bug tracker number and a version number" do
    lambda { ruby_bug { }          }.should raise_error(ArgumentError)
    lambda { ruby_bug("#1234") { } }.should raise_error(ArgumentError)
  end

  it "sets the name of the guard to :ruby_bug" do
    ruby_bug("#1234", "1.8.6") { }
    @guard.name.should == :ruby_bug
  end

  it "calls #unregister even when an exception is raised in the guard block" do
    @guard.should_receive(:unregister)
    lambda do
      ruby_bug("", "") { raise Exception }
    end.should raise_error(Exception)
  end
end
