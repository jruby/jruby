require File.expand_path('../../ant_spec_helper', __FILE__)
require 'rake'
# Do this hoo-hah with import because we don't want Rake's version of "import".
def import(*args); java_import(*args); end

describe Ant, "Rake helpers" do
  include Ant::RSpec::AntExampleGroup

  it "should set FileLists as task attributes by joining them with commas" do
    ant = Ant.new
    ant.property :name => "files", :value => FileList['*.*']
    expect(ant.properties["files"]).to match(/,/)
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
    expect(ant.properties).not_to include("foo")

    task :initial
    ant_task :ant => :initial do
      property :name => "foo", :value => "bar"
    end
    expect(Rake::Task[:ant]).not_to be_nil
    expect(Rake::Task[:ant].prerequisites).to contain_exactly("initial")
    Rake::Task[:ant].invoke

    expect(ant.properties["foo"]).to eq("bar")
  end
end
