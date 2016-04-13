require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe :to_s, shared: true do

  it "returns a string representation with same order as each()" do
    h = { a: [1, 2], b: -2, d: -6, nil => nil }

    pairs = []
    h.each do |key, value|
      pairs << key.inspect + '=>' + value.inspect
    end

    str = '{' + pairs.join(', ') + '}'
    h.send(@method).should == str
  end

  it "calls inspect on keys and values" do
    key = mock('key')
    val = mock('val')
    key.should_receive(:inspect).and_return('key')
    val.should_receive(:inspect).and_return('val')

    { key => val }.send(@method).should == '{key=>val}'
  end

  it "handles hashes with recursive values" do
    x = {}
    x[0] = x
    x.send(@method).should == '{0=>{...}}'

    x = {}
    y = {}
    x[0] = y
    y[1] = x
    x.send(@method).should == "{0=>{1=>{...}}}"
    y.send(@method).should == "{1=>{0=>{...}}}"
  end

  it "returns a tainted string if self is tainted and not empty" do
    {}.taint.send(@method).tainted?.should be_false
    { nil => nil }.taint.send(@method).tainted?.should be_true
  end

  it "returns an untrusted string if self is untrusted and not empty" do
    {}.untrust.send(@method).untrusted?.should be_false
    { nil => nil }.untrust.send(@method).untrusted?.should be_true
  end
end
