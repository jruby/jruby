require File.dirname(__FILE__) + "/../spec_helper"

describe "A class that implements Comparable" do
  it "still uses .equals for ==" do
    # JRUBY-6967
    expect(java.util.Date.new.==('foo')).to eq(false)
  end
end