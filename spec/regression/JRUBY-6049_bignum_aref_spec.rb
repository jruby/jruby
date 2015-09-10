require 'rspec'

describe 'JRUBY-6049: Bignum#[]' do
  it 'handles negative value correctly' do
    expect((-1<<100)[(1<<100)]).to eq(1)
  end

  it 'normalizes Bignum argument' do
    expect(((1<<100) + 3)[(1<<100).coerce(1).first]).to eq(1)
  end
end
