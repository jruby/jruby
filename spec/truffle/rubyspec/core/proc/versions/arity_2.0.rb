describe "Proc#arity" do
  before :each do
    @p = ProcSpecs::Arity.new
  end

  it "returns -1 for a block taking |a = 0, *b| argument " do
    @p.arity_check { |a = 0, *b| }.should == -1
  end

  it "returns -1 for a lambda taking one optional argument" do
    lambda { |a = 0| }.arity.should == -1
  end

  it "returns -2 for a lambda taking |a, b = 0| argument " do
    lambda { |a, b = 0| }.arity.should == -2
  end

  it "returns -2 for a lambda taking |a, b = 0, c = 0| argument " do
    lambda { |a, b = 0, c = 0| }.arity.should == -2
  end

  it "returns -2 for a lambda taking |(a, b), c = 0| argument " do
    lambda { |(a, b), c = 0| }.arity.should == -2
  end

  it "returns 0 for a Proc taking one optional argument" do
    Proc.new { |a = 0| }.arity.should == 0
  end

  it "returns 1 for a Proc taking |a, b = 0| argument " do
    Proc.new { |a, b = 0| }.arity.should == 1
  end

  it "returns 1 for a Proc taking |a, b = 0, c = 0| argument " do
    Proc.new { |a, b = 0, c = 0| }.arity.should == 1
  end

  it "returns 1 for a Proc taking |(a, b), c = 0| argument " do
    Proc.new { |(a, b), c = 0| }.arity.should == 1
  end
end
