require_relative '../../spec_helper'
require_relative 'fixtures/classes'
require_relative 'shared/iterable_and_tolerating_size_increasing'

describe "Array#find_index" do
  it "returns the index of the first element == to object" do
    x = mock('3')
    def x.==(obj) 3 == obj; end

    [2, x, 3, 1, 3, 1].find_index(3).should == 1
    [2, 3.0, 3, x, 1, 3, 1].find_index(x).should == 1
  end

  it "returns 0 if first element == to object" do
    [2, 1, 3, 2, 5].find_index(2).should == 0
  end

  it "returns size-1 if only last element == to object" do
    [2, 1, 3, 1, 5].find_index(5).should == 4
  end

  it "returns nil if no element == to object" do
    [2, 1, 1, 1, 1].find_index(3).should == nil
  end

  it "compares an Integer with Float elements using Float#==" do
    [0.5, 1.0, 2.5].find_index(1).should == 1
    [0.5, 1.5, 2.5].find_index(1).should == nil
  end

  it "compares a Float with Integer elements using Integer#==" do
    [0, 1, 2].find_index(1.0).should == 1
    [0, 1, 2].find_index(1.5).should == nil
  end

  it "considers 0.0 and -0.0 equal" do
    [1.0, 0.0, 2.0].find_index(-0.0).should == 1
  end

  it "finds a NaN element only if it is the same object" do
    nan = Float::NAN
    [1.0, nan, 2.0].find_index(nan).should == 1
    [1.0, nan, 2.0].find_index(0.0 / 0.0).should == nil
  end

  it "uses a redefined Integer#==" do
    result = ArraySpecs.with_redefined_integer_equality { [1, 2, 3, 2].find_index(42) }
    result.should == 1
  end

  it "accepts a block instead of an argument" do
    [4, 2, 1, 5, 1, 3].find_index {|x| x < 2}.should == 2
  end

  it "ignores the block if there is an argument" do
    -> {
      [4, 2, 1, 5, 1, 3].find_index(5) {|x| x < 2}.should == 3
    }.should complain(/given block not used/)
  end

  describe "given no argument and no block" do
    it "produces an Enumerator" do
      [].find_index.should.instance_of?(Enumerator)
    end
  end

  it_behaves_like :array_iterable_and_tolerating_size_increasing, :find_index
end
