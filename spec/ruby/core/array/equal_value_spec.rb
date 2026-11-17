require_relative '../../spec_helper'
require_relative 'fixtures/classes'
require_relative 'shared/eql'

describe "Array#==" do
  it_behaves_like :array_eql, :==

  it "compares with an equivalent Array-like object using #to_ary" do
    obj = mock('array-like')
    obj.should_receive(:respond_to?).at_least(1).with(:to_ary).and_return(true)
    obj.should_receive(:==).with([1]).at_least(1).and_return(true)

    ([1] == obj).should == true
    ([[1]] == [obj]).should == true
    ([[[1], 3], 2] == [[obj, 3], 2]).should == true

    # recursive arrays
    arr1 = [[1]]
    arr1 << arr1
    arr2 = [obj]
    arr2 << arr2
    (arr1 == arr2).should == true
    (arr2 == arr1).should == true
  end

  it "returns false if any corresponding elements are not #==" do
    a = ["a", "b", "c"]
    b = ["a", "b", "not equal value"]
    a.should_not == b

    c = mock("c")
    c.should_receive(:==).and_return(false)
    ["a", "b", c].should_not == a
  end

  it "returns true if corresponding elements are #==" do
    [].should == []
    ["a", "c", 7].should == ["a", "c", 7]

    [1, 2, 3].should == [1.0, 2.0, 3.0]

    obj = mock('5')
    obj.should_receive(:==).and_return(true)
    [obj].should == [5]
  end

  it "calls a redefined Integer#== on Integers that are not identical" do
    ruby_exe(<<~RUBY).should == "[true, true]\n"
      class Integer
        def ==(other) = true
      end
      p [[1, 2**40] == [3, 2**40 + 1], [2**70] == [2**70 + 1]]
    RUBY
  end

  it "calls Integer#== from a module prepended to Integer" do
    ruby_exe(<<~RUBY).should == "true\n"
      Integer.prepend(Module.new { def ==(other) = true })
      p [1, 2**40] == [3, 2**40 + 1]
    RUBY
  end

  it "calls a redefined Float#== on Floats that are not identical" do
    ruby_exe(<<~RUBY).should == "true\n"
      class Float
        def ==(other) = true
      end
      p [1.5, 1e300] == [2.5, 2e300]
    RUBY
  end

  it "returns false for Floats that are NaN and not identical" do
    [0.0 / 0.0].should_not == [0.0 / 0.0]
  end

  it "returns true for 0.0 and -0.0" do
    [0.0].should == [-0.0]
  end

  # See https://bugs.ruby-lang.org/issues/1720
  it "returns true for [NaN] == [NaN] because Array#== first checks with #equal? and NaN.equal?(NaN) is true" do
    [Float::NAN].should == [Float::NAN]
  end
end
