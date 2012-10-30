require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require 'enumerator'

describe "Enumerable#each_slice" do
  before :each do
    @enum = EnumerableSpecs::Numerous.new(7,6,5,4,3,2,1)
    @sliced = [[7,6,5],[4,3,2],[1]]
  end

  it "passes element groups to the block" do
    acc = []
    @enum.each_slice(3){|g| acc << g}.should be_nil
    acc.should == @sliced
  end

  it "raises an Argument Error if there is not a single parameter > 0" do
    lambda{ @enum.each_slice(0){}    }.should raise_error(ArgumentError)
    lambda{ @enum.each_slice(-2){}   }.should raise_error(ArgumentError)
    lambda{ @enum.each_slice{}       }.should raise_error(ArgumentError)
    lambda{ @enum.each_slice(2,2){}  }.should raise_error(ArgumentError)
  end

  it "tries to convert n to an Integer using #to_int" do
    acc = []
    @enum.each_slice(3.3){|g| acc << g}.should == nil
    acc.should == @sliced

    obj = mock('to_int')
    obj.should_receive(:to_int).and_return(3)
    @enum.each_slice(obj){|g| break g.length}.should == 3
  end

  it "works when n is >= full length" do
    full = @enum.to_a
    acc = []
    @enum.each_slice(full.length){|g| acc << g}
    acc.should == [full]
    acc = []
    @enum.each_slice(full.length+1){|g| acc << g}
    acc.should == [full]
  end

  it "yields only as much as needed" do
    cnt = EnumerableSpecs::EachCounter.new(1, 2, :stop, "I said stop!", :got_it)
    cnt.each_slice(2) {|g| break 42 if g[0] == :stop }.should == 42
    cnt.times_yielded.should == 4
  end

  ruby_version_is "1.8.7" do
    it "returns an enumerator if no block" do
      e = @enum.each_slice(3)
      e.should be_an_instance_of(enumerator_class)
      e.to_a.should == @sliced
    end

    it "gathers whole arrays as elements when each yields multiple" do
      multi = EnumerableSpecs::YieldsMulti.new
      multi.each_slice(2).to_a.should == [[[1, 2], [3, 4, 5]], [[6, 7, 8, 9]]]
    end
  end
end
