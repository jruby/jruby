require 'rspec'

describe "A native method with no-simple arity should not NPE" do
  it "an invoker has proper parameters" do
    expect([].method(:shuffle).parameters).to eq([[:rest]])
  end
end
