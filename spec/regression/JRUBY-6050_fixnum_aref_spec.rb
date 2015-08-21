require 'rspec'

describe 'JRUBY-6050: Fixnum#[]' do
  it 'normalizes Bignum argument' do
    bn = (1<<100).coerce(1).first
    expect(3[bn]).to eq(1)
  end
end
