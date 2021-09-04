require 'rspec'

describe "Enumerable#drop" do
  context "when called on an Enumerator" do
    let(:enumerator) { (1..10).to_a.each_slice(3)}
    it "should behave as if it is called on an Enumerable" do
      expect(enumerator.drop(2)).to eq([[7,8,9],[10]])
    end
  end
end