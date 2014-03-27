require 'rspec'

if RUBY_VERSION >= "1.9"
  describe "Random.rand(1.0)" do
    it "returns a Float" do
      Random.rand(1.0).should be_kind_of(Float)
    end
    it "does not always equal 0" do
      (1..1000).map{|x| Random.rand(1.0)}.all?(&:zero?).should be_false
    end
  end

  describe "Random::DEFAULT" do
    it "returns a Float for a Float argument" do
      Random.rand(1.0).should be_kind_of(Float)
    end
  end
end
