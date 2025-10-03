require 'rspec'

describe "Array#repeated_permutation" do
  it "has arity one" do
    expect([].method(:repeated_permutation).arity).to eq(1)
  end
end
