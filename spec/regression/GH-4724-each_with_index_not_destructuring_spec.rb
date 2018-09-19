require 'rspec'

describe "Enumerable#each_with_index" do
  it "will destructure array to first element if one param present" do
    %i(foo).each_with_index {|v| expect(v).to eq(:foo) }
  end
end
