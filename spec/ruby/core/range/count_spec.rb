require_relative '../../spec_helper'

describe "Range#count" do
  it "returns Infinity for beginless ranges without arguments or blocks" do
    inf = Float::INFINITY
    eval("('a'...)").count.should == inf
    eval("(7..)").count.should == inf
    (...'a').count.should == inf
    (...nil).count.should == inf
    (..10.0).count.should == inf
  end

  it "accepts an argument for comparison using ==" do
    (1..10).count(2).should == 1
  end

  it "uses a block for comparison" do
    (1..10).count{|x| x%2==0 }.should == 5
  end

  it "ignores the block when given an argument" do
    -> {
      (1..10).count(4){|x| x%2==0 }.should == 1
    }.should complain(/given block not used/)
  end
end
