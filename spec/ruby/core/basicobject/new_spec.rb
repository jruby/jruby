require File.expand_path('../../../spec_helper', __FILE__)

describe "BasicObject.new" do
  it "returns an instance of BasicObject" do
    # BasicObject cannot participate in .should matchers. Further,
    # there is no #class method on BasicObject. Hence, we can only
    # infer that we have an instance of BasicObject.
    (Object === BasicObject.new).should be_false
  end
end
