require 'rspec'

describe "Regexp" do

  it "does not raise SyntaxError when parsing utf8 string with redundant regexp escape" do
    expect{ r = /[\§]/ }.not_to raise_error
    expect(/[\§]/).to eq(/[§]/)
  end
end