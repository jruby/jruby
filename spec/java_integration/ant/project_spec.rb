require File.expand_path('../../ant_spec_helper', __FILE__)

describe Ant, "project" do
  include Ant::RSpec::AntExampleGroup
  before :each do
    @ant = example_ant :name => "spec project", :description => "spec description"
  end

  it "should have the 'basedir' set" do
    # expand_path is used to avoid / and \\ mismatch on Windows
    expect(File.expand_path(@ant.project.base_dir.path)).to eq(Dir::tmpdir)
  end

  it "should have a project helper created" do
    expect(@ant.project.get_reference(Ant::ProjectHelper::PROJECTHELPER_REFERENCE)).to be_kind_of(Ant::ProjectHelper)
  end

  it "should have a logger set" do
    expect(@ant.project.build_listeners).to_not be_empty
  end

  it "should have a name and description" do
    expect(@ant.project.name).to eq("spec project")
    expect(@ant.project.description).to eq("spec description")
  end
end
