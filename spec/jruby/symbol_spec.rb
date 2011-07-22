describe "#inspect" do
  it "returns :$ruby" do
    :$ruby.inspect.should == ":$ruby"
  end
  
  it "returns :$ruby" do
    :$_.inspect.should == ":$_"
  end
end

describe "#inject" do
  context "with &:+ symbol" do
     it "returns 15" do
       [1,2,3,4,5].inject(&:+).should == 15
     end
   end
end
