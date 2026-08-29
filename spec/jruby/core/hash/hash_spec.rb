require 'rspec'

# https://github.com/jruby/jruby/issues/1590
describe 'Compute a hash-code for this hash' do

  it 'makes the computed hash-code value different from the hash size' do
    expect({:baz => :baz, :qux => :qux}.hash).not_to eq(2)
  end

  it 'returns the same hash-code for hash with the same content' do
    hash_code = {:baz => :baz, :qux => :qux}.hash
    expect({:baz => :baz, :qux => :qux}.hash).to eq(hash_code)
  end
end
