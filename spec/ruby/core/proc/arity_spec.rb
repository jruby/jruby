require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)

ruby_bug "#5694", "1.9.3" do
  language_version __FILE__, "arity"
end

describe "Proc#arity" do
  before :each do
    @p = ProcSpecs::Arity.new
  end

  ruby_version_is ""..."1.9" do
    it "returns -1 for a block taking no arguments" do
      @p.arity_check { 1 }.should == -1
    end
  end

  ruby_version_is "1.9" do
    it "returns 0 for a block taking no arguments" do
      @p.arity_check { 1 }.should == 0
    end
  end

  it "returns 0 for a block taking || arguments" do
    @p.arity_check { || }.should == 0
  end

  it "returns 1 for a block taking |a| arguments" do
    @p.arity_check { |a| }.should == 1
  end

  it "returns 1 for a block taking |a, | arguments" do
    @p.arity_check { |a, | }.should == 1
  end

  it "returns -2 for a block taking |a, *| arguments" do
    @p.arity_check { |a, *| }.should == -2
  end

  it "returns -2 for a block taking |a, *b| arguments" do
    @p.arity_check { |a, *b| }.should == -2
  end

  it "returns -3 for a block taking |a, b, *c| arguments" do
    @p.arity_check { |a, b, *c| }.should == -3
  end

  it "returns 2 for a block taking |a, b| arguments" do
    @p.arity_check { |a, b| }.should == 2
  end

  it "returns 3 for a block taking |a, b, c| arguments" do
    @p.arity_check { |a, b, c| }.should == 3
  end

  it "returns -1 for a block taking |*| arguments" do
    @p.arity_check { |*| }.should == -1
  end

  it "returns -1 for a block taking |*a| arguments" do
    @p.arity_check { |*a| }.should == -1
  end

  ruby_version_is ""..."1.9" do
    it "returns 2 for a block taking |(a, b)| arguments" do
      @p.arity_check { |(a, b)| }.should == 2
    end

    it "returns -2 for a block taking |(a, *)| arguments" do
      @p.arity_check { |(a, *)| }.should == -2
    end

    it "returns -2 for a block taking |(a, *b)| arguments" do
      @p.arity_check { |(a, *b)| }.should == -2
    end
  end

  ruby_version_is "1.9" do
    it "returns 1 for a block taking |(a, b)| arguments" do
      @p.arity_check { |(a, b)| }.should == 1
    end

    it "returns 1 for a block taking |(a, *)| arguments" do
      @p.arity_check { |(a, *)| }.should == 1
    end

    it "returns 1 for a block taking |(a, *b)| arguments" do
      @p.arity_check { |(a, *b)| }.should == 1
    end
  end

  it "returns 2 for a block taking |a, (b, c)| arguments" do
    @p.arity_check { |a, (b, c)| }.should == 2
  end

  it "returns 2 for a block taking |a, (b, *c)| arguments" do
    @p.arity_check { |a, (b, *c)| }.should == 2
  end

  it "returns 2 for a block taking |(a, b), c| arguments" do
    @p.arity_check { |(a, b), c| }.should == 2
  end

  it "returns -2 for a block taking |(a, b), *c| arguments" do
    @p.arity_check { |(a, b), *c| }.should == -2
  end

  it "returns 2 for a block taking |(a, *b), c| arguments" do
    @p.arity_check { |(a, *b), c| }.should == 2
  end
end
