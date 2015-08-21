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
end if RUBY_VERSION > "1.9"