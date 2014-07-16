require 'rspec'

describe "ArrayJavaProxy#to_a" do
  it "succesfully converts arrays containing nil" do
    [nil].to_java.to_a.should == [nil]
  end
end
