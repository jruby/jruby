require 'rspec'

describe "JRUBY-6612: multiplication" do
  it "works when other is Long.MIN_VALUE" do
    LONG_MIN = -(2**63)
    expect(-1*LONG_MIN).to eq(LONG_MIN*(-1))
    expect(-1*LONG_MIN).to eq(-LONG_MIN)
  end
end