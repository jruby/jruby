require 'rspec'

describe 'JRUBY-6050: Fixnum#[]' do
  it 'normalizes Bignum argument' do
    bn = (1<<100).coerce(1).first
    3[bn].should == 1
  end
end
