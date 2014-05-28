require File.expand_path('../../../spec_helper', __FILE__)

describe "Integer#downto [stop] when self and stop are Fixnums" do
  it "does not yield when stop is greater than self" do
    result = []
    5.downto(6) { |x| result << x }
    result.should == []
  end

  it "yields once when stop equals self" do
    result = []
    5.downto(5) { |x| result << x }
    result.should == [5]
  end

  it "yields while decreasing self until it is less than stop" do
    result = []
    5.downto(2) { |x| result << x }
    result.should == [5, 4, 3, 2]
  end

  it "yields while decreasing self until it less than ceil for a Float endpoint" do
    result = []
    9.downto(1.3) {|i| result << i}
    3.downto(-1.3) {|i| result << i}
    result.should == [9, 8, 7, 6, 5, 4, 3, 2, 3, 2, 1, 0, -1]
  end

  it "raises an ArgumentError for invalid endpoints" do
    lambda {1.downto("A") {|x| p x } }.should raise_error(ArgumentError)
    lambda {1.downto(nil) {|x| p x } }.should raise_error(ArgumentError)
  end

  it "returns an Enumerator" do
    result = []

    enum = 5.downto(2)
    enum.each { |i| result << i }

    result.should == [5, 4, 3, 2]
  end
end
