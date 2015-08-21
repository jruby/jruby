describe "Array#sample" do
  it "accepts PRNG instance" do
    expect([1, 2, 3].sample(1, :random => Random.new(0))).not_to be_nil
  end

  it "accepts PRNG class" do
    expect([1, 2, 3].sample(1, :random => Random)).not_to be_nil
  end
end

describe "Array#shuffle" do
  it "accepts PRNG instance" do
    expect([1, 2, 3].shuffle(:random => Random.new(0)).size).to eq(3)
  end

  it "accepts PRNG class" do
    expect([1, 2, 3].shuffle(:random => Random).size).to eq(3)
  end
end
