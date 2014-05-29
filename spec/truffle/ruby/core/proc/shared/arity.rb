describe "Proc#arity" do
  before :each do
    @p = ProcSpecs::Arity.new
  end

  it "returns -1 for a block taking one optional argument" do
    @p.arity_check { |a = 0| }.should == 0
  end

  it "returns -1 for a block taking |a = 0, *b| argument " do
    @p.arity_check { |a = 0, *b| }.should == -1
  end

  it "returns -2 for a block taking |a, b = 0| argument " do
    @p.arity_check { |a, b = 0| }.should == 1
  end

  it "returns -2 for a block taking |a, b = 0, c = 0| argument " do
    @p.arity_check { |a, b = 0, c = 0| }.should == 1
  end

  it "returns -2 for a block taking |(a, b), c = 0| argument " do
    @p.arity_check { |(a, b), c = 0| }.should == 1
  end
end
