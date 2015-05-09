describe ".const_get" do

  module Example
    class Foo
      Bar = "bar"
    end
  end

  context "with leading colons" do
    it "finds the toplevel constant" do
      Object.const_get("::Example").should == Example
    end

    it "works with arbitrarily nested constants" do
      Object.const_get("::Example::Foo::Bar").should == "bar"
    end
  end

end
