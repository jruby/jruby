describe :string_equal_value, :shared => true do
  it "returns true if self <=> string returns 0" do
    'hello'.send(@method, 'hello').should == true
  end

  it "returns false if self <=> string does not return 0" do
    "more".send(@method, "MORE").should == false
    "less".send(@method, "greater").should == false
  end

  it "ignores subclass differences" do
    a = "hello"
    b = StringSpecs::MyString.new("hello")

    a.send(@method, b).should == true
    b.send(@method, a).should == true
  end
end
