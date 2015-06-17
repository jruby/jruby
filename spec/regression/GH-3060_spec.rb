require 'rspec'

describe "An empty %i{}" do
  it "will not crash" do
    %i{}.should eq([])
  end
end
