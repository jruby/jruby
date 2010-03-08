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

  it "should heed :if and :unless conditions" do
    message = ""
    ant = Ant.new :output_level => 0 do
      property :name => "foo", :value => "defined"
      target :will_never_execute, :if => "not.defined" do
        message << "will_never_execute?"
      end

      target :also_will_never_execute, :unless => "foo" do
        message << "also_will_never_execute"
      end

      target :may_execute, :if => "bar" do
        message << "executed"
      end
    end

    ant.execute_target(:will_never_execute)
    ant.execute_target(:also_will_never_execute)
    ant.execute_target(:may_execute)
    message.should == ""

    ant.property :name => "bar", :value => "defined"
    ant.execute_target(:may_execute)
    message.should_not == ""
  end

  it "should execute target tasks and non-tasks in order" do
    ant = Ant.new :output_level => 0 do
      target :foo do
        property :name => "bar", :value => "true"
        ant.properties["bar"].should == "true"
      end
    end
    ant.execute_target(:foo)
  end
end
