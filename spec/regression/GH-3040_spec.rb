require 'rspec'

describe "A native method with no-simple arity should not NPE" do
  it "an invoker has proper parameters" do
    [].method(:shuffle).parameters.should eq([[:rest]])
  end
end
