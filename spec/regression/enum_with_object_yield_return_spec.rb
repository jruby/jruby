require 'rspec'

describe "Enumerator#with_object" do
  it "provides the yielder's return value to its block" do
    foo = Enumerator.new { |y| expect(y.yield).to eq(42) }
    foo.each_with_object({}) { 42 }
  end
end