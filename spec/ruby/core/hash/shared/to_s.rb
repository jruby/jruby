require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe :to_s, :shared => true do

  it "returns a string representation with same order as each()" do
    h = new_hash(:a => [1, 2], :b => -2, :d => -6, nil => nil)

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

    new_hash(key => val).send(@method).should == '{key=>val}'
  end

  it "handles hashes with recursive values" do
    x = new_hash
    x[0] = x
    x.send(@method).should == '{0=>{...}}'

    x = new_hash
    y = new_hash
    x[0] = y
    y[1] = x
    x.send(@method).should == "{0=>{1=>{...}}}"
    y.send(@method).should == "{1=>{0=>{...}}}"
  end

  # Recursive hash keys are disallowed on 1.9
  ruby_version_is ""..."1.9" do
    it "handles hashes with recursive keys" do
      x = new_hash
      x[x] = 0
      x.send(@method).should == '{{...}=>0}'

      x = new_hash
      x[x] = x
      x.send(@method).should == '{{...}=>{...}}'


      x = new_hash
      y = new_hash
      x[y] = 0
      y[x] = 1
      x.send(@method).should == "{{{...}=>1}=>0}"
      y.send(@method).should == "{{{...}=>0}=>1}"

      x = new_hash
      y = new_hash
      x[y] = x
      y[x] = y
      x.send(@method).should == "{{{...}=>{...}}=>{...}}"
      y.send(@method).should == "{{{...}=>{...}}=>{...}}"
    end
  end
end
