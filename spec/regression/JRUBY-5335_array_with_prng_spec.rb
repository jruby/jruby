describe "Array#choice" do
  it "doesn't exist in 1.9" do
    if RUBY_VERSION >= "1.9.2"
      [].respond_to?(:choice).should == false
    else
      [].choice.should be_nil
    end
  end
end

if RUBY_VERSION >= "1.9.2"
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
end
