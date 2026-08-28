# coding: utf-8

require 'rspec'

# https://github.com/jruby/jruby/issues/3402
describe 'String#encode with :replace option' do
  it 'returns correct value' do
    str = "testing\xC2".encode("UTF-8", :invalid => :replace, :undef => :replace, :replace => "foo123")
    expect(str).to eq "testingfoo123"
  end
end

