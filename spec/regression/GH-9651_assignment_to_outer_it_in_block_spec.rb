# https://github.com/jruby/jruby/issues/9651

describe "Assignment to an outer local variable named `it` inside a block" do
  it "writes the outer variable instead of creating a block-local shadow" do
    it = 0
    3.times { it = 5 }
    expect(it).to eq(5)
  end

  it "supports op-assignment to the outer variable" do
    it = 0
    3.times { it += 1 }
    expect(it).to eq(3)
  end

  it "writes the outer variable when the block declares explicit parameters" do
    it = 0
    [1, 2].each { |e| it += e }
    expect(it).to eq(3)
  end

  it "reads the outer variable correctly" do
    it = 0
    seen = []
    3.times { seen << it }
    expect(seen).to eq([0, 0, 0])
  end

  it "shadows (rather than writes) an implicit `it` of an outer block" do
    outer_seen = nil
    inner_seen = nil
    [10].each { it.to_s; [nil].each { it = 5; inner_seen = it }; outer_seen = it }
    expect(inner_seen).to eq(5)
    expect(outer_seen).to eq(10)
  end
end
