require 'rspec'

describe "An empty %i{}" do
  it "will not crash" do
    expect(%i{}).to eq([])
  end
end
