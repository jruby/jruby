require File.expand_path('../../ant_spec_helper', __FILE__)

describe Ant, '.load' do
  include Ant::RSpec::AntExampleGroup

  before :each do
    @previous_java_home = ENV['JAVA_HOME'] || ENV_JAVA['java.home']

    if (TestHelper::WINDOWS)
      ENV['JAVA_HOME'] = '/C:/java6'
    else
      ENV['JAVA_HOME'] = '/System/Library/Frameworks/JavaVM.framework/Home'
    end
    @tools_jar = "#{ENV['JAVA_HOME']}/lib/tools.jar"
    @classes_zip = "#{ENV['JAVA_HOME']}/lib/classes.zip"

    Ant.instance_eval do
      remove_const(:JAVA_HOME) rescue nil
    end
  end

  after :each do
    ENV['JAVA_HOME'] = @previous_java_home
    Ant.instance_eval do
      remove_const(:JAVA_HOME)
      const_set(:JAVA_HOME, @previous_java_home)
    end
  end

  it "adds tools.jar to the CLASSPATH when JAVA_HOME is set and it exists" do
    stubs_file!
    Ant.load
    $CLASSPATH.should include("file:#{@tools_jar}")
  end

  it "adds classes.zip to the CLASSPATH when JAVA_HOME is set and it exists" do
    stubs_file!
    Ant.load
    $CLASSPATH.should include("file:#{@classes_zip}")
  end

  def stubs_file!
    File.stub!(:exist?).and_return false
    File.should_receive(:exist?).with(ENV['JAVA_HOME']).and_return true
    File.should_receive(:exist?).with(@tools_jar).and_return true
    File.should_receive(:exist?).with(@classes_zip).and_return true
  end
end

describe Ant, ".new" do
  include Ant::RSpec::AntExampleGroup

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

describe Ant do
  include Ant::RSpec::AntExampleGroup

  before :each do
    @ant = example_ant
  end

  it "should define methods corresponding to ant tasks" do
    if RUBY_VERSION =~ /\A1\.8/
      @ant.methods.should include("java", "antcall", "property", "import", "path", "patternset")
    else
      @ant.methods.should include(:java, :antcall, :property, :import, :path, :patternset)
    end
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

describe Ant, '.ant' do
  it "prefers $ANT_HOME to $PATH" do
    if ENV['ANT_HOME']
      hide_ant_from_path
      lambda { Ant.ant(:basedir => File.join(File.dirname(__FILE__), '..', '..', '..')) }.
        should_not raise_error
    else
      pending '$ANT_HOME is not set'
    end
  end
end
