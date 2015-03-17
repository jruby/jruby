require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Enumerable#find_index" do
  before :each do
    @elements = [2, 4, 6, 8, 10]
    @numerous = EnumerableSpecs::Numerous.new(*@elements)
    @yieldsmixed = EnumerableSpecs::YieldsMixed2.new
  end

  it "passes each entry in enum to block while block when block is false" do
    visited_elements = []
    @numerous.find_index do |element|
      visited_elements << element
      false
    end
    visited_elements.should == @elements
  end

  it "returns nil when the block is false" do
    @numerous.find_index {|e| false }.should == nil
  end

  it "returns the first index for which the block is not false" do
    @elements.each_with_index do |element, index|
      @numerous.find_index {|e| e > element - 1 }.should == index
    end
  end

  it "returns the first index found" do
    repeated = [10, 11, 11, 13, 11, 13, 10, 10, 13, 11]
    numerous_repeat = EnumerableSpecs::Numerous.new(*repeated)
    repeated.each do |element|
      numerous_repeat.find_index(element).should == element - 10
    end
  end

  it "returns nil when the element not found" do
    @numerous.find_index(-1).should == nil
  end

  it "ignores the block if an argument is given" do
    @numerous.find_index(-1) {|e| true }.should == nil
  end

  it "returns an Enumerator if no block given" do
    @numerous.find_index.should be_an_instance_of(enumerator_class)
  end

  describe "without block" do
    it "gathers whole arrays as elements when each yields multiple" do
      @yieldsmixed.find_index([0, 1, 2]).should == 3
    end
  end

  describe "with block" do
    before(:each) do
      ScratchPad.record []
    end

    after(:each) do
      ScratchPad.clear
    end

    describe "given a single yield parameter" do
      it "passes first element to the parameter" do
        @yieldsmixed.find_index {|a| ScratchPad << a; false }
        ScratchPad.recorded.should == EnumerableSpecs::YieldsMixed2.first_yields
      end
    end

    describe "given a greedy yield parameter" do
      it "passes a gathered array to the parameter" do
        @yieldsmixed.find_index {|*args| ScratchPad << args; false }
        ScratchPad.recorded.should == EnumerableSpecs::YieldsMixed2.greedy_yields
      end
    end
  end
end
