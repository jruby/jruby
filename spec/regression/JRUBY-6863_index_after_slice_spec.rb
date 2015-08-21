# -*- coding: utf-8 -*-
require 'rspec'

describe 'JRUBY-6863' do
  let(:str) do 
    str = "あいうえおかきくけこ"
  end

  subject do
    str.slice!(3..-1) # => "えおかきくけこ"
  end

  it 'String#index without args' do 
    # See http://jira.codehaus.org/browse/JRUBY-xxxx
    expect(subject.index(/[^ ]/)).to eq(0)
  end

  it 'String#index with args' do
    expect(subject.index(/[^ ]/, 2)).to eq(2)
  end

  it 'String#rindex without args' do 
    expect(subject.rindex(/[^ ]/)).to eq(6)
  end

  it 'String#rindex with args' do 
    expect(subject.rindex(/[^ ]/, 2)).to eq(2)
  end
end
