require File.expand_path('../../../spec_helper', __FILE__)

describe "Float#*" do
  it "returns self multiplied by other" do
    (4923.98221 * 2).should be_close(9847.96442, TOLERANCE)
    (6712.5 * 0.25).should be_close(1678.125, TOLERANCE)
    (256.4096 * bignum_value).should be_close(2364961134621118431232.000, TOLERANCE)
  end
end
