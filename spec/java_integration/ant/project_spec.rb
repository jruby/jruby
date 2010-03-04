require File.expand_path('../../ant_spec_helper', __FILE__)

describe Ant, "project", :type => :ant do
  before :each do
    @ant = Ant.new :name => "spec project", :description => "spec description", :basedir => "/tmp", :run => false
  end

  it "should have the 'basedir' set" do
    @ant.project.base_dir.path.should == "/tmp"
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
