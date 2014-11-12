require 'rspec'

# https://github.com/jruby/jruby/issues/1590
describe 'Compute a hash-code for this hash' do

  it 'makes the computed hash-code value different from the hash size' do
    {:baz => :baz, :qux => :qux}.hash.should_not == 2
  end

  it 'returns the same hash-code for hash with the same content' do
    hash_code = {:baz => :baz, :qux => :qux}.hash
    {:baz => :baz, :qux => :qux}.hash.should == hash_code
  end
end
