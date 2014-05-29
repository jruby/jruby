require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require 'enumerator'

describe "Enumerable#each_cons" do
  before :each do
    @enum = EnumerableSpecs::Numerous.new(4,3,2,1)
    @in_threes = [[4,3,2],[3,2,1]]
  end

  it "passes element groups to the block" do
    acc = []
    @enum.each_cons(3){|g| acc << g}.should be_nil
    acc.should == @in_threes
  end

  it "raises an Argument Error if there is not a single parameter > 0" do
    lambda{ @enum.each_cons(0){}    }.should raise_error(ArgumentError)
    lambda{ @enum.each_cons(-2){}   }.should raise_error(ArgumentError)
    lambda{ @enum.each_cons{}       }.should raise_error(ArgumentError)
    lambda{ @enum.each_cons(2,2){}  }.should raise_error(ArgumentError)
  end

  it "tries to convert n to an Integer using #to_int" do
    acc = []
    @enum.each_cons(3.3){|g| acc << g}.should == nil
    acc.should == @in_threes

    obj = mock('to_int')
    obj.should_receive(:to_int).and_return(3)
    @enum.each_cons(obj){|g| break g.length}.should == 3
  end

  it "works when n is >= full length" do
    full = @enum.to_a
    acc = []
    @enum.each_cons(full.length){|g| acc << g}
    acc.should == [full]
    acc = []
    @enum.each_cons(full.length+1){|g| acc << g}
    acc.should == []
  end

  it "yields only as much as needed" do
    cnt = EnumerableSpecs::EachCounter.new(1, 2, :stop, "I said stop!", :got_it)
    cnt.each_cons(2) {|g| break 42 if g[-1] == :stop }.should == 42
    cnt.times_yielded.should == 3
  end

  ruby_version_is "1.8.7" do
    it "returns an enumerator if no block" do
      e = @enum.each_cons(3)
      e.should be_an_instance_of(enumerator_class)
      e.to_a.should == @in_threes
    end

    it "gathers whole arrays as elements when each yields multiple" do
      multi = EnumerableSpecs::YieldsMulti.new
      multi.each_cons(2).to_a.should == [[[1, 2], [3, 4, 5]], [[3, 4, 5], [6, 7, 8, 9]]]
    end
  end
end
