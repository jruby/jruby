require 'rspec'

if RUBY_VERSION >= "1.9"
  describe "Random.rand(1.0)" do
    it "returns a Float" do
      expect(Random.rand(1.0)).to be_kind_of(Float)
    end

    it "does not always equal 0" do
      expect(
        (1..1000).map{|x| Random.rand(1.0)}.all?(&:zero?)
      ).to be false
    end
  end

  describe "Random::DEFAULT" do
    it "returns a Float for a Float argument" do
      expect(Random.rand(1.0)).to be_kind_of(Float)
    end
  end
end
