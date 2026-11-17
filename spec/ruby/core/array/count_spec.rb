require_relative '../../spec_helper'
require_relative 'fixtures/classes'
require_relative 'shared/iterable_and_tolerating_size_increasing'

describe "Array#count" do
  it "returns the number of elements" do
    [:a, :b, :c].count.should == 3
  end

  it "returns the number of elements that equal the argument" do
    [:a, :b, :b, :c].count(:b).should == 2
  end

  it "compares an Integer with Float elements using Float#==" do
    [1.0, 1, 2.5, 1.0].count(1).should == 3
    [0.5, 1.5, 2.5].count(1).should == 0
  end

  it "compares a Float with Integer elements using Integer#==" do
    [1, 1.0, 2, 1].count(1.0).should == 3
    [0, 1, 2].count(1.5).should == 0
  end

  it "considers 0.0 and -0.0 equal" do
    [0.0, 1.0, -0.0].count(-0.0).should == 2
  end

  it "counts NaN elements only if they are the same object" do
    nan = Float::NAN
    [nan, 1.0, nan, 0.0 / 0.0].count(nan).should == 2
    [nan, 1.0, nan].count(0.0 / 0.0).should == 0
  end

  it "uses a redefined Integer#==" do
    result = ArraySpecs.with_redefined_integer_equality { [1, 2, 3, 2].count(42) }
    result.should == 2
  end

  it "returns the number of element for which the block evaluates to true" do
    [:a, :b, :c].count { |s| s != :b }.should == 2
  end

  it "ignores the block if there is an argument" do
    -> {
      [:a, :b, :b, :c].count(:b) { |e| e.size > 10 }.should == 2
    }.should complain(/given block not used/)
  end

  context "when a block argument given" do
    it_behaves_like :array_iterable_and_tolerating_size_increasing, :count
  end
end
