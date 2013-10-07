describe "Array#sample" do
  it "accepts PRNG instance" do
    [1, 2, 3].sample(1, :random => Random.new(0)).should_not be_nil
  end

  it "accepts PRNG class" do
    [1, 2, 3].sample(1, :random => Random).should_not be_nil
  end
end

describe "Array#shuffle" do
  it "accepts PRNG instance" do
    [1, 2, 3].shuffle(:random => Random.new(0)).size.should == 3
  end

  it "accepts PRNG class" do
    [1, 2, 3].shuffle(:random => Random).size.should == 3
  end
end
