describe "GH-1962: Kernel::Array" do
  it "coerces Array-like objects that define method_missing" do
    o = Object.new
    def o.method_missing(name, *args)
      []
    end

    expect(Array(o)).to eq([])
  end

  it "coerces Array-like objects that define to_ary" do
    o = Object.new
    def o.to_ary
      []
    end

    expect(Array(o)).to eq([])
  end

  it "coerces Array-like objects that define to_a" do
    o = Object.new
    def o.to_a
      []
    end

    expect(Array(o)).to eq([])
  end
end