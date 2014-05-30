require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is "1.8.7" do
  describe "Enumerable#max_by" do
    it "returns an enumerator if no block" do
      EnumerableSpecs::Numerous.new(42).max_by.should be_an_instance_of(enumerator_class)
    end

    it "returns nil if #each yields no objects" do
      EnumerableSpecs::Empty.new.max_by {|o| o.nonesuch }.should == nil
    end

    it "returns the object for whom the value returned by block is the largest" do
      EnumerableSpecs::Numerous.new(*%w[1 2 3]).max_by {|obj| obj.to_i }.should == '3'
      EnumerableSpecs::Numerous.new(*%w[three five]).max_by {|obj| obj.length }.should == 'three'
    end

    it "returns the object that appears first in #each in case of a tie" do
      a, b, c = '1', '2', '2'
      EnumerableSpecs::Numerous.new(a, b, c).max_by {|obj| obj.to_i }.should equal(b)
    end

    it "uses max.<=>(current) to determine order" do
      a, b, c = (1..3).map{|n| EnumerableSpecs::ReverseComparable.new(n)}

      # Just using self here to avoid additional complexity
      EnumerableSpecs::Numerous.new(a, b, c).max_by {|obj| obj }.should == a
    end

    it "is able to return the maximum for enums that contain nils" do
      enum = EnumerableSpecs::Numerous.new(nil, nil, true)
      enum.max_by {|o| o.nil? ? 0 : 1 }.should == true
      enum.max_by {|o| o.nil? ? 1 : 0 }.should == nil
    end

    it "gathers whole arrays as elements when each yields multiple" do
      multi = EnumerableSpecs::YieldsMulti.new
      multi.max_by {|e| e.size}.should == [6, 7, 8, 9]
    end
  end
end
