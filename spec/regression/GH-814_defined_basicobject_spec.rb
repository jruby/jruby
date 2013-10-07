describe "defined?(::BasicObject)" do
  it "returns \"constant\"" do
    defined?(::BasicObject).should == "constant"
  end
end
