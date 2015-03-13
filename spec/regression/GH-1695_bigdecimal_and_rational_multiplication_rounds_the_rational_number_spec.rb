require 'bigdecimal'

# https://github.com/jruby/jruby/issues/1695
describe 'BigDecimal#*' do
  it 'returns correct value' do
    (BigDecimal.new('100') * Rational(1, 100)).to_i.should == 1
    (BigDecimal.new('100') * Rational(49, 100)).to_i.should == 49
    (BigDecimal.new('100') * Rational(50, 100)).to_i.should == 50
  end
end

