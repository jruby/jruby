# coding: utf-8
require 'rspec'

describe 'JRUBY-6933: A COWed String being split by another String' do
  it 'properly matches the split token' do
    "[âœ“:checkbox_name]" =~ /\[(.*?)\]/      
    ary = $1.split(':')
    ary[0].should == "âœ“"
    ary[1].should == "checkbox_name"
  end
end
