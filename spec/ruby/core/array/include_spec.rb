require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe "Array#include?" do
  it "returns true if object is present, false otherwise" do
    [1, 2, "a", "b"].include?("c").should == false
    [1, 2, "a", "b"].include?("a").should == true
  end

  it "determines presence by using element == obj" do
    o = mock('')

    [1, 2, "a", "b"].include?(o).should == false

    def o.==(other); other == 'a'; end

    [1, 2, o, "b"].include?('a').should == true

    [1, 2.0, 3].include?(2).should == true
  end

  it "calls == on elements from left to right until success" do
    key = "x"
    one = mock('one')
    two = mock('two')
    three = mock('three')
    one.should_receive(:==).any_number_of_times.and_return(false)
    two.should_receive(:==).any_number_of_times.and_return(true)
    three.should_not_receive(:==)
    ary = [one, two, three]
    ary.include?(key).should == true
  end

  it "compares an Integer with Float elements using Float#==" do
    [0.5, 1.0, 2.5].include?(1).should == true
    [0.5, 1.5, 2.5].include?(1).should == false
  end

  it "compares a Float with Integer elements using Integer#==" do
    [0, 1, 2].include?(1.0).should == true
    [0, 1, 2].include?(1.5).should == false
  end

  it "considers 0.0 and -0.0 equal" do
    [1.0, 0.0, 2.0].include?(-0.0).should == true
  end

  it "finds a NaN element only if it is the same object" do
    nan = Float::NAN
    [1.0, nan, 2.0].include?(nan).should == true
    [1.0, nan, 2.0].include?(0.0 / 0.0).should == false
  end

  it "uses a redefined Integer#==" do
    result = ArraySpecs.with_redefined_integer_equality { [1, 2, 3].include?(42) }
    result.should == true
  end
end
