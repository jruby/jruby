require File.expand_path('../../ant_spec_helper', __FILE__)

describe Ant, "tasks:" do
  include Ant::RSpec::AntExampleGroup

  before :all do
    # The single example ant project these specs will validate
    @output = output = "ant-file#{rand}.txt"
    @message = message = ""
    @ant = example_ant :basedir => "." do
      property :name => "jar", :value => "spec-test.jar"
      property :name => "dir", :value => "build"
      taskdef :name => "jarjar", :classname => "com.tonicsystems.jarjar.JarJarTask",
        :classpath => "${basedir}/test/target/jarjar.jar"

      target :jar do
        jar :destfile => "${jar}", :compress => "true", :index => "true" do
          fileset :dir => "${dir}"
        end
      end

      target :jarjar do
        jarjar :destfile => "${jar}", :compress => "true" do
          fileset :dir => "${dir}"
          zipfileset :src => "./lib/jruby.jar"
        end
      end

      macrodef :name => "greet" do
        attribute :name => "msg"
        sequential do
          echo :message => "Hello @{msg}", :file => "#{output}"
        end
      end

      target :greet do
        greet :msg => "Ant"
      end

      target :rubygreet do
        message << "Hello Ruby!"
      end
    end
  end

  after :all do
    File.unlink(@output) if File.exist?(@output)
  end

  before :each do
    @message.replace("")
  end

  describe "jar" do
    subject do
      @ant.project.targets["jar"]
    end

    it { should have_structure([{:_name => "jar", :destfile => "spec-test.jar", :compress => "true", :index => "true",
                                 :_children => [ { :_name => "fileset", :dir => "build" }] }]) }

    it { should have_configured_structure([{:_type => "org.apache.tools.ant.taskdefs.Jar",
                                            :_children => [{:_type => "org.apache.tools.ant.types.FileSet"}] }]) }

  end

  describe "jarjar" do
    subject do
      @ant.project.targets["jarjar"]
    end

    it { should have_structure([{:_name => "jarjar", :destfile => "spec-test.jar", :compress => "true",
                                 :_children => [ { :_name => "fileset", :dir => "build" },
                                                 { :_name => "zipfileset", :src => "./lib/jruby.jar" }] }]) }

    it { should have_configured_structure([{:_type => "com.tonicsystems.jarjar.JarJarTask",
                                            :_children => [{:_type => "org.apache.tools.ant.types.FileSet"},
                                                           {:_type => "org.apache.tools.ant.types.ZipFileSet"}] }]) }
  end

  describe "macrodef" do
    it "should be defined and invokable from a target" do
      @ant.execute_target(:greet)
      File.read(@output).should == "Hello Ant"
    end
  end

  describe "rubygreet" do
    it "should execute the code block when the target is executed" do
      @message.should == ""
      @ant.execute_target(:rubygreet)
      @message.should == "Hello Ruby!"
    end
  end
end
