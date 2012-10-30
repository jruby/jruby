require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#trace_var" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:trace_var)
  end
end

describe "Kernel.trace_var" do
  it "needs to be reviewed for spec completeness"
end
