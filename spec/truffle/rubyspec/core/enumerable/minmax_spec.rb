require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is "1.8.7" do
  describe "Enumerable#minmax" do
    before :each do
      @enum = EnumerableSpecs::Numerous.new(6, 4, 5, 10, 8)

      @strs = EnumerableSpecs::Numerous.new("333", "2", "60", "55555", "1010", "111")
    end

    it "min should return the minimum element" do
      @enum.minmax.should == [4, 10]
      @strs.minmax.should == ["1010", "60" ]
    end

    it "returns [nil, nil] for an empty Enumerable" do
      EnumerableSpecs::Empty.new.minmax.should == [nil, nil]
    end

    it "raises an ArgumentError when elements are incomparable" do
      lambda do
        EnumerableSpecs::Numerous.new(11,"22").minmax
      end.should raise_error(ArgumentError)
      lambda do
        EnumerableSpecs::Numerous.new(11,12,22,33).minmax{|a, b| nil}
      end.should raise_error(ArgumentError)
    end

    ruby_version_is ""..."1.9" do
      it "raises a NoMethodError for elements without #<=>" do
        lambda do
          EnumerableSpecs::Numerous.new(Object.new, Object.new).minmax
        end.should raise_error(NoMethodError)
      end
    end

    ruby_version_is "1.9" do
      it "raises a NoMethodError for elements without #<=>" do
        lambda do
          EnumerableSpecs::Numerous.new(BasicObject.new, BasicObject.new).minmax
        end.should raise_error(NoMethodError)
      end
    end

    it "return the minimun when using a block rule" do
      @enum.minmax {|a,b| b <=> a }.should == [10, 4]
      @strs.minmax {|a,b| a.length <=> b.length }.should == ["2", "55555"]
    end

    it "gathers whole arrays as elements when each yields multiple" do
      multi = EnumerableSpecs::YieldsMulti.new
      multi.minmax.should == [[1, 2], [6, 7, 8, 9]]
    end
  end
end
