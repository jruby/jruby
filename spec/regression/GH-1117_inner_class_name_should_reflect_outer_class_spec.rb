describe "A Java inner class's name" do
  it "reflects its scoping within an outer class" do
    # Using a fairly obscure class here to ensure we have not initialized it other ways.
    Java::JavaUtil::ResourceBundle::Control.name.should == "Java::JavaUtil::ResourceBundle::Control"
    java.util.ResourceBundle::Control.name.should == "Java::JavaUtil::ResourceBundle::Control"
  end
end
