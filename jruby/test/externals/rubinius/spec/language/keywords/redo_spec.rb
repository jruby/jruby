require File.dirname(__FILE__) + '/../../spec_helper'

describe "The redo statement" do
  it "raises LocalJumpError if used not within block or while/for loop" do
    def x; redo; end
    should_raise(LocalJumpError) { x }
  end

  it "restarts block execution if used whithin block" do
    a = []
    lambda {
      a << 1
      redo if a.size < 2
      a << 2
    }.call
    a.should == [1, 1, 2]
  end
end
