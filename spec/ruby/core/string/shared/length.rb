describe :string_length, :shared => true do
  it "returns the length of self" do
    "".send(@method).should == 0
    "\x00".send(@method).should == 1
    "one".send(@method).should == 3
    "two".send(@method).should == 3
    "three".send(@method).should == 5
    "four".send(@method).should == 4
  end
end
