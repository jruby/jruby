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
      begin
        remove_const(:JAVA_HOME)
        const_set(:JAVA_HOME, @previous_java_home)
      rescue NameError
        # ignore, JAVA_HOME constant is not necessarily set now
      end
    end
  end

  it "adds tools.jar to the CLASSPATH when JAVA_HOME is set and it exists" do
    stub_File!
    Ant.load
    expect($CLASSPATH).to include("file:#{@tools_jar}")
  end

  it "adds classes.zip to the CLASSPATH when JAVA_HOME is set and it exists" do
    stub_File!
    Ant.load
    expect($CLASSPATH).to include("file:#{@classes_zip}")
  end

  def stub_File!
    allow(File).to receive(:exist?).and_return false
    expect(File).to receive(:exist?).with(ENV['JAVA_HOME']).and_return true
    expect(File).to receive(:exist?).with(@tools_jar).and_return true
    expect(File).to receive(:exist?).with(@classes_zip).and_return true
  end
end

describe Ant, ".new" do
  include Ant::RSpec::AntExampleGroup

  it "can be instantiated with a block" do
    klass = nil

    Ant.new do
      klass = self.class
    end

    expect(klass).to eq(Ant)
  end

  it "can be instantiated with a block whose single argument receives the Ant instance" do
    klass = nil
    ant_klass = nil

    Ant.new do |ant|
      klass = self.class
      ant_klass = ant.class
    end

    expect(klass).not_to eq(Ant)
    expect(ant_klass).to eq(Ant)
  end

  it "should execute top-level tasks as it encounters them" do
    Ant.new do |ant|
      expect(ant.properties["foo"]).to_not eq("bar")
      ant.property :name => "foo", :value => "bar"
      expect(ant.properties["foo"]).to eq("bar")
    end
  end

  it "should have a valid location" do
    expect(File).to be_exist(Ant.new.location.file_name)
  end
end

describe Ant do
  include Ant::RSpec::AntExampleGroup

  before :each do
    @ant = example_ant
  end

  it "should define methods corresponding to ant tasks" do
    expect(@ant.methods).to include(:java, :antcall, :property, :import, :path, :patternset)
  end

  it "should execute the default target" do
    @ant.target("default") { property :name => "spec", :value => "example" }
    @ant.project.default = "default"
    @ant.execute_default
    expect(@ant.properties["spec"]).to eq("example")
  end

  it "should execute the specified target" do
    @ant.target("a") { property :name => "a", :value => "true" }
    @ant.target("b") { property :name => "b", :value => "true" }
    @ant.execute_target("a")
    expect(@ant.properties["a"]).to eq("true")
    @ant["b"].execute
    expect(@ant.properties["b"]).to eq("true")
  end

  it "should raise when a bogus target is executed" do
    expect { @ant["bogus"].execute }.to raise_error(RuntimeError)
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
    expect(@ant.properties["msg"]).to eq("hello")
  end
end

describe Ant, '.ant' do
  it "prefers $ANT_HOME to $PATH" do
    if ENV['ANT_HOME']
      hide_ant_from_path
      expect { Ant.ant(:basedir => File.join(File.dirname(__FILE__), '..', '..', '..')) }.
        to_not raise_error
    else
      skip '$ANT_HOME is not set'
    end
  end
end
