describe "GH-1962: Kernel::Array" do
  it "coerces Array-like objects that only define method_missing" do
    o = Object.new
    def o.method_missing(name, *args)
      []
    end

    expect(Array(o)).to eq([])
  end
end
