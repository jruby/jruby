require 'rspec'

describe 'JRUBY-6554: \r at end of string' do
  it "does not cause SyntaxError when eval'd" do
    expect {eval "{:a => '\r'}"}.not_to raise_error
  end
end