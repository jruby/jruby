require 'rspec'

describe "JRUBY-6612: multiplication" do
  it "works when other is Long.MIN_VALUE" do
    LONG_MIN = -(2**63)
    (-1*LONG_MIN).should == LONG_MIN*(-1)
    (-1*LONG_MIN).should == -LONG_MIN
  end
end