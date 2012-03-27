require 'rspec'

describe 'JRUBY-6554: \r at end of string' do
  it "does not cause SyntaxError when eval'd" do
    lambda {eval "{:a => '\r'}"}.should_not raise_error SyntaxError
  end
end