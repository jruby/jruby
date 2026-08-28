require 'rspec'

describe "ArrayJavaProxy#to_a" do
  it "succesfully converts arrays containing nil" do
    expect([nil].to_java.to_a).to eq([nil])
  end
end
