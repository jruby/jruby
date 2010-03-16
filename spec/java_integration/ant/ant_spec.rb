require File.expand_path('../../ant_spec_helper', __FILE__)

describe Ant, "new", :type => :ant do
  it "can be instantiated with a block" do
    Ant.new do
      self.class.should == Ant
    end
  end

  it "can be instantiated with a block whose single argument receives the Ant instance" do
    Ant.new do |ant|
      self.class.should_not == Ant
      ant.class.should == Ant
    end
  end

  it "should execute top-level tasks as it encounters them" do
    Ant.new do |ant|
      ant.properties["foo"].should_not == "bar"
      ant.property :name => "foo", :value => "bar"
      ant.properties["foo"].should == "bar"
    end
  end

  it "should have a valid location" do
    File.should be_exist(Ant.new.location.file_name)
  end
end

describe Ant, :type => :ant do
  before :each do
    @ant = example_ant
  end

  it "should define methods corresponding to ant tasks" do
    @ant.methods.should include("java", "antcall", "property", "import", "path", "patternset")
  end

  it "should execute the default target" do
    @ant.target("default") { property :name => "spec", :value => "example" }
    @ant.project.default = "default"
    @ant.execute_default
    @ant.properties["spec"].should == "example"
  end

  it "should execute the specified target" do
    @ant.target("a") { property :name => "a", :value => "true" }
    @ant.target("b") { property :name => "b", :value => "true" }
    @ant.execute_target("a")
    @ant.properties["a"].should == "true"
    @ant["b"].execute
    @ant.properties["b"].should == "true"
  end

  it "should raise when a bogus target is executed" do
    lambda { @ant["bogus"].execute }.should raise_error
  end

  it "should handle -Dkey=value arguments from the command-line" do
    @ant.project.default = "help"
    @ant.process_arguments(["-Dcommand.line.msg=hello", "help"], false)
    @ant.define_tasks do
      target :help do
        property :name => "msg", :value => "${command.line.msg}"
      end
    end
    @ant.run
    @ant.properties["msg"].should == "hello"
  end
end
