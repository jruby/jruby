require File.expand_path('../../ant_spec_helper', __FILE__)

describe Ant, "targets", :type => :ant do
  it "should delay executing tasks in targets until the target is executed" do
    ant = Ant.new :name => "foo", :output_level => 0 do
      target :foo do
        property :name => "foo", :value => "bar"
      end
    end
    ant.properties["foo"].should_not == "bar"
    ant.execute_target(:foo)
    ant.properties["foo"].should == "bar"
  end

  it "#ant should accumulate targets and tasks in the same global project" do
    ant do
      target :a
    end
    ant do
      target :b
    end
    ant.project.targets.keys.to_a.should include("a", "b")
  end
end
