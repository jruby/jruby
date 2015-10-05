require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby'

describe "RubyArray#set" do
  it "sets the index specified to the value specified" do
    ary = [0,1,2]
    ary_r = JRuby.reference(ary)
    ary_r.set(1, 5)
    expect(ary[1]).to eq(5)
  end

  it "returns the value previously at the given index" do
    ary = [0,1,2]
    ary_r = JRuby.reference(ary)
    prev = ary_r.set(1, 5)
    expect(prev).to eq(1)
  end
end