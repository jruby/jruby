# coding: utf-8
require 'rspec'

describe 'JRUBY-6933: A COWed String being split by another String' do
  it 'properly matches the split token' do
    "[âœ“:checkbox_name]" =~ /\[(.*?)\]/      
    ary = $1.split(':')
    expect(ary[0]).to eq("âœ“")
    expect(ary[1]).to eq("checkbox_name")
  end
end
