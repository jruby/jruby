require 'bigdecimal'

# https://github.com/jruby/jruby/issues/1695
describe 'BigDecimal#*' do
  it 'returns correct value' do
    expect((BigDecimal.new('100') * Rational(1, 100)).to_i).to eq(1)
    expect((BigDecimal.new('100') * Rational(49, 100)).to_i).to eq(49)
    expect((BigDecimal.new('100') * Rational(50, 100)).to_i).to eq(50)
  end
end

