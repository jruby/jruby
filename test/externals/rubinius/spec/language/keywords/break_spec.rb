require File.dirname(__FILE__) + '/../../spec_helper'

describe "The break statement" do
  it "raises LocalJumpError if used not within block or while/for loop" do
    def x; break; end
    should_raise(LocalJumpError) { x }
  end

  it "ends block execution if used whithin block" do
    a = []
    lambda {
      a << 1
      break
      a << 2
    }.call
    a.should == [1]
  end

  it "causes block to return value passed to break" do
    lambda { break 123; 456 }.call.should == 123
  end

  it "causes block to return nil if no value passed to break" do
    lambda { break; 456 }.call.should == nil
  end
end
