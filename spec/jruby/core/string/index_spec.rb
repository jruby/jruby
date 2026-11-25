# encoding: utf-8
# GH-1087: String#index with regex and multi-byte characters returns wrong index
# https://github.com/jruby/jruby/issues/1087

describe 'String#index given a Regexp and an index past the last character' do
  it "returns nil" do
    # without multibyte
    str = "strings - strings"
    expect(str.index(/\b/, 18)).to eq(nil)
    
    # with multibyte
    str = "ßt®íngß — ßt®íngß"
    expect(str.index(/\b/, 18)).to eq(nil)
  end
end


describe 'JRUBY-6863' do
  let(:str) do 
    str = "あいうえおかきくけこ"
  end

  subject do
    (+str).slice!(3..-1) # => "えおかきくけこ"
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
