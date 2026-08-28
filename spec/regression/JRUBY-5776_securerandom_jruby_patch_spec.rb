require 'securerandom'

describe 'JRUBY-5776: SecureRandom#random_number' do
  it 'works' do
    expect(SecureRandom.random_number(2**128)).to be >= 0
    expect(SecureRandom.random_number(0)).to be <= 1.0
    expect(SecureRandom.random_number).to be <= 1.0
  end
end
