require 'rspec'

describe "The Proc class" do
  it "should not be directly allocatable" do
    lambda do
      Proc.allocate
    end.should raise_error(TypeError)
  end
end
