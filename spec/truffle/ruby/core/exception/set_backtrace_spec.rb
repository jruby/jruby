require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)

describe "Exception#set_backtrace" do
  it "allows the user to set the backtrace to any array" do
    err = RuntimeError.new
    err.set_backtrace ["unhappy"]
    err.backtrace.should == ["unhappy"]
  end

  it "allows the user to set the backtrace from a rescued exception" do
    bt  = ExceptionSpecs::Backtrace.backtrace
    err = RuntimeError.new

    err.set_backtrace bt
    err.backtrace.should == bt
  end
end
