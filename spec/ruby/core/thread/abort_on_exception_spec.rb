require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Thread#abort_on_exception" do
  it "is changeable to true or false" do
    Thread.abort_on_exception = true
    Thread.abort_on_exception.should == true
    Thread.abort_on_exception = false
    Thread.abort_on_exception.should == false
  end
end

describe "Thread#abort_on_exception=" do
  it "needs to be reviewed for spec completeness"
end

describe "Thread.abort_on_exception" do
  it "needs to be reviewed for spec completeness"
end

describe "Thread.abort_on_exception=" do
  it "needs to be reviewed for spec completeness"
end
