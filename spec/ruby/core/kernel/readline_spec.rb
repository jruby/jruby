require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe "Kernel#readline" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:readline)
  end

  it "calls ARGF.readline" do
    ARGF.should_receive(:readline).and_return("spec")
    readline.should == "spec"
  end
end

describe "Kernel.readline" do
  it "needs to be reviewed for spec completeness"
end
