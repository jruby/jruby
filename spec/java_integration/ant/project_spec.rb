require File.expand_path('../../ant_spec_helper', __FILE__)

describe Ant, "project" do
  include Ant::RSpec::AntExampleGroup
  before :each do
    @ant = example_ant :name => "spec project", :description => "spec description"
  end

  it "should have the 'basedir' set" do
    # expand_path is used to avoid / and \\ mismatch on Windows
    File.expand_path(@ant.project.base_dir.path).should == Dir::tmpdir
  end

  it "should have a project helper created" do
    @ant.project.get_reference(Ant::ProjectHelper::PROJECTHELPER_REFERENCE).should be_kind_of(Ant::ProjectHelper)
  end

  it "should have a logger set" do
    @ant.project.build_listeners.should_not be_empty
  end

  it "should have a name and description" do
    @ant.project.name.should == "spec project"
    @ant.project.description.should == "spec description"
  end
end
