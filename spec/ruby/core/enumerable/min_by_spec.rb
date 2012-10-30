require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is "1.8.7" do
  describe "Enumerable#min_by" do
    it "returns an enumerator if no block" do
      EnumerableSpecs::Numerous.new(42).min_by.should be_an_instance_of(enumerator_class)
    end

    it "returns nil if #each yields no objects" do
      EnumerableSpecs::Empty.new.min_by {|o| o.nonesuch }.should == nil
    end


    it "returns the object for whom the value returned by block is the largest" do
      EnumerableSpecs::Numerous.new(*%w[3 2 1]).min_by {|obj| obj.to_i }.should == '1'
      EnumerableSpecs::Numerous.new(*%w[five three]).min_by {|obj| obj.length }.should == 'five'
    end

    it "returns the object that appears first in #each in case of a tie" do
      a, b, c = '2', '1', '1'
      EnumerableSpecs::Numerous.new(a, b, c).min_by {|obj| obj.to_i }.should equal(b)
    end

    it "uses min.<=>(current) to determine order" do
      a, b, c = (1..3).map{|n| EnumerableSpecs::ReverseComparable.new(n)}

      # Just using self here to avoid additional complexity
      EnumerableSpecs::Numerous.new(a, b, c).min_by {|obj| obj }.should == c
    end

    it "is able to return the maximum for enums that contain nils" do
      enum = EnumerableSpecs::Numerous.new(nil, nil, true)
      enum.min_by {|o| o.nil? ? 0 : 1 }.should == nil
      enum.min_by {|o| o.nil? ? 1 : 0 }.should == true
    end

    it "gathers whole arrays as elements when each yields multiple" do
      multi = EnumerableSpecs::YieldsMulti.new
      multi.min_by {|e| e.size}.should == [1, 2]
    end
  end
end
