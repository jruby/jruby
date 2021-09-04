describe ".const_get" do

  module Example
    class Foo
      Bar = "bar"
    end
  end

  context "with leading colons" do
    it "finds the toplevel constant" do
      expect(Object.const_get("::Example")).to eq(Example)
    end

    it "works with arbitrarily nested constants" do
      expect(Object.const_get("::Example::Foo::Bar")).to eq("bar")
    end
  end

end
