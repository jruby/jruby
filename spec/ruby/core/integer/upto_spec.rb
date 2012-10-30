require File.expand_path('../../../spec_helper', __FILE__)

describe "Integer#upto [stop] when self and stop are Fixnums" do
  it "does not yield when stop is less than self" do
    result = []
    5.upto(4) { |x| result << x }
    result.should == []
  end

  it "yields once when stop equals self" do
    result = []
    5.upto(5) { |x| result << x }
    result.should == [5]
  end

  it "yields while increasing self until it is less than stop" do
    result = []
    2.upto(5) { |x| result << x }
    result.should == [2, 3, 4, 5]
  end

  it "yields while increasing self until it is greater than floor of a Float endpoint" do
    result = []
    9.upto(13.3) {|i| result << i}
    -5.upto(-1.3) {|i| result << i}
    result.should == [9,10,11,12,13,-5,-4,-3,-2]
  end

  it "raises an ArgumentError for non-numeric endpoints" do
    lambda { 1.upto("A") {|x| p x} }.should raise_error(ArgumentError)
    lambda { 1.upto(nil) {|x| p x} }.should raise_error(ArgumentError)
  end

  ruby_version_is "" ... "1.8.7" do
    it "raises a LocalJumpError when no block given" do
      lambda { 2.upto(5) }.should raise_error(LocalJumpError)
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator" do
      result = []

      enum = 2.upto(5)
      enum.each { |i| result << i }

      result.should == [2, 3, 4, 5]
    end
  end
end
