require 'rspec'

describe 'Rational' do

  let(:subject) { "1".to_r }

  describe '#to_s' do
    it "returns a string with US-ASCII encoding" do
      expect(subject.to_s.encoding).to eq(Encoding::US_ASCII)
    end
  end
end
