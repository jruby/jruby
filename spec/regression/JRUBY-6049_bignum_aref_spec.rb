require 'rspec'

describe 'JRUBY-6049: Bignum#[]' do
  it 'handles negative value correctly' do
    (-1<<100)[(1<<100)].should == 1
  end

  it 'normalizes Bignum argument' do
    ((1<<100) + 3)[(1<<100).coerce(1).first].should == 1
  end
end
