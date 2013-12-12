require 'rspec'

describe "Array#repeated_permutation" do
  it "has arity one" do
    [].method(:repeated_permutation).arity.should == 1
  end
end if RUBY_VERSION >= "1.9"
