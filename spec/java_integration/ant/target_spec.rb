require File.expand_path('../../ant_spec_helper', __FILE__)

describe Ant, "targets" do
  include Ant::RSpec::AntExampleGroup

  it "should delay executing tasks in targets until the target is executed" do
    ant = example_ant :name => "foo" do
      target :foo do
        property :name => "foo", :value => "bar"
      end
    end
    expect(ant.properties["foo"]).not_to eq("bar")
    ant.execute_target(:foo)
    expect(ant.properties["foo"]).to eq("bar")
  end

  it "#ant should accumulate targets and tasks in the same global project" do
    ant do
      target :a
    end
    ant do
      target :b
    end
    expect(ant.project.targets.keys.to_a).to include("a", "b")
  end

  it "should heed :if and :unless conditions" do
    message = ""
    ant = example_ant do
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
    expect(message).to be_empty

    ant.property :name => "bar", :value => "defined"
    ant.execute_target(:may_execute)
    expect(message).to_not be_empty
  end

  it "should execute target tasks and non-tasks in order" do
    bar = nil
    ant = example_ant do
      target :foo do
        property :name => "bar", :value => "true"
        bar = ant.properties["bar"]
      end
    end
    ant.execute_target(:foo)
    expect(bar).to eq("true")
  end

  it "should be executable if it doesn't have a block" do
    bar = nil
    ant = example_ant do
      target :foo do
        property :name => "bar", :value => "true"
        bar = ant.properties["bar"]
      end
      target :bar, :depends => :foo
    end
    ant.execute_target(:bar)
    expect(bar).to eq("true")
  end

  it "does not support antcall for calling other targets" do
    ant = example_ant do
      target :foo
      target :bar do
        antcall :target => :foo
      end
    end
    expect {
      ant.execute_target(:bar)
    }.to raise_error(Java::OrgApacheToolsAnt::BuildException)
  end

  it "does not support ant for calling other buildfiles" do
    a = example_ant do
      target :foo
      target :bar do
        ant :antfile => "some-file", :target => :foo
      end
    end
    expect { ant.execute_target(:bar) }.to raise_error(RuntimeError)
  end
end
