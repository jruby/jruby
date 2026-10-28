describe "A subclass of a Data class that includes a module in its define block" do
  before :each do
    mod = Module.new { def greet = "hello" }
    @base = Data.define(:x, :y) { include mod }
    @sub = Class.new(@base)
  end

  it "returns member values via direct accessor calls" do
    obj = @sub.new(x: 1, y: 2)

    expect(obj.x).to eq(1)
    expect(obj.y).to eq(2)
  end

  it "returns member values via #public_send" do
    obj = @sub.new(x: 1, y: 2)

    expect(obj.public_send(:x)).to eq(1)
    expect(obj.public_send(:y)).to eq(2)
  end

  it "returns member values for a grand-subclass" do
    subsub = Class.new(@sub)
    obj = subsub.new(x: 1, y: 2)

    expect(obj.public_send(:x)).to eq(1)
    expect(obj.public_send(:y)).to eq(2)
  end

  it "returns member values from #with" do
    obj = @sub.new(x: 1, y: 2)
    obj_with = obj.with(y: 99)

    expect(obj_with.x).to eq(1)
    expect(obj_with.y).to eq(99)
    expect(obj_with.class).to eq(@sub)
  end
end
