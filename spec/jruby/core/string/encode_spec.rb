# coding: utf-8

require 'base64'
require 'rspec'

# https://github.com/jruby/jruby/issues/3402
describe 'String#encode with :replace option' do
  it 'returns correct value' do
    str = "testing\xC2".encode("UTF-8", :invalid => :replace, :undef => :replace, :replace => "foo123")
    expect(str).to eq "testingfoo123"
  end
end

describe "A badly-encoded UTF-8 String reencoded with replacements as UTF-16 " do
  it "completes for all inputs" do
    random = Random.new
    # We obviously can't test all valid inputs, but we use the script from #2856 to try
    10_000.times do
      data = random.bytes(1000)
      data.force_encoding("UTF-8")
      data = data.encode("UTF-16", :undef => :replace, :invalid => :replace, :replace => '')

      expect(data).to_not eq(nil)
    end
  end
end
