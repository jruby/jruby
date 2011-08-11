require 'securerandom'

describe 'JRUBY-5776: SecureRandom#random_number' do
  it 'works' do
    SecureRandom.random_number(2**128).should >= 0
    SecureRandom.random_number(0).should <= 1.0
    SecureRandom.random_number.should <= 1.0
  end
end
