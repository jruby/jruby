require File.expand_path('../../ant_spec_helper', __FILE__)
require 'rake'
# Do this hoo-hah with import because we don't want Rake's version of "import".
def import(*args); java_import(*args); end
require 'ant/rake'

describe Ant, "Rake helpers" do
  include Ant::RSpec::AntExampleGroup

  it "should set FileLists as task attributes by joining them with commas" do
    ant = Ant.new
    ant.property :name => "files", :value => FileList['*.*']
    ant.properties["files"].should =~ /,/
  end

end

describe Ant, "Rake #ant_task" do
  include Ant::RSpec::AntExampleGroup
  include Rake::DSL if defined?(Rake::DSL)

  before :each do
    @app = Rake.application
    Rake.application = Rake::Application.new
  end

  after :each do
    Rake.application = @app
  end

  it "should create a Rake task whose body defines Ant tasks" do
    ant.properties.should_not include("foo")

    task :initial
    ant_task :ant => :initial do
      property :name => "foo", :value => "bar"
    end
    Rake::Task[:ant].should_not be_nil
    Rake::Task[:ant].prerequisites.should == ["initial"]
    Rake::Task[:ant].invoke

    ant.properties["foo"].should == "bar"
  end
end
