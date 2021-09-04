require 'rspec'

describe "A bunch of hash methods" do
  let(:hash) { {a: :b} }

  it "can handle standard arities for any?" do
    hash.any? { |k| expect(k).to eq([:a, :b]) }
    hash.any? { |k,v| expect(k).to eq(:a); expect(v).to eq(:b) }
  end

  it "can handle standard arities for delete_if" do
    hash.delete_if { |k| expect(k).to eq(:a) }
    hash.delete_if { |k,v| expect(k).to eq(:a); expect(v).to eq(:b) }
  end

  it "can handle standard arities for each" do
    hash.each { |k,v| expect(k).to eq(:a); expect(v).to eq(:b) }
  end

  it "can handle standard arities for select" do
    hash.select { |k| expect(k).to eq(:a) }
    hash.select { |k,v| expect(k).to eq(:a); expect(v).to eq(:b) }
  end
  
  it "can handle standard arities for select!" do
    hash.select! { |k| expect(k).to eq(:a) }
    hash.select! { |k,v| expect(k).to eq(:a); expect(v).to eq(:b) }
  end
end
