require 'rspec'

describe "The Proc class" do
  it "should not be directly allocatable" do
    expect do
      Proc.allocate
    end.to raise_error(TypeError)
  end
end
