require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is "1.9" do
  describe "Enumerable#chunk" do
    it "raises an ArgumentError if called without a block" do
      lambda do
        EnumerableSpecs::Numerous.new.chunk
      end.should raise_error(ArgumentError)
    end

    it "returns an Enumerator if given a block" do
      EnumerableSpecs::Numerous.new.chunk {}.should be_an_instance_of(enumerator_class)
    end

    it "yields each element of the Enumerable to the block" do
      yields = []
      EnumerableSpecs::Numerous.new.chunk {|e| yields << e}.to_a
      EnumerableSpecs::Numerous.new.to_a.should == yields
    end

    it "returns an Enumerator of 2-element Arrays" do
      EnumerableSpecs::Numerous.new.chunk {|e| true}.each do |a|
        a.should be_an_instance_of(Array)
        a.size.should == 2
      end
    end

    it "sets the first element of each sub-Array to the return value of the block" do
      EnumerableSpecs::Numerous.new.chunk {|e| -e }.each do |a|
        a.first.should == -a.last.first
      end
    end

    it "sets the last element of each sub-Array to the consecutive values for which the block returned the first element" do
      ret = EnumerableSpecs::Numerous.new(5,5,2,3,4,5,7,1,9).chunk {|e| e >= 5 }.to_a
      ret[0].last.should == [5, 5]
      ret[1].last.should == [2, 3, 4]
      ret[2].last.should == [5, 7]
      ret[3].last.should == [1]
      ret[4].last.should == [9]
    end
  end
end
