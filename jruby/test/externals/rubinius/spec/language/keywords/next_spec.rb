require File.dirname(__FILE__) + '/../../spec_helper'

describe "The redo statement" do
  it "raises LocalJumpError if used not within block or while/for loop" do
    def x; next; end
    should_raise(LocalJumpError) { x }
  end

  it "ends block execution if used whithin block" do
    a = []
    lambda {
      a << 1
      next
      a << 2
    }.call
    a.should == [1]
  end

  it "causes block to return nil" do
    lambda { 123; next; 456 }.call.should == nil
  end

  it "accepts argument but does nothing with it in blocks" do
    lambda { 123; next 234; 345 }.call.should == nil
  end
end
