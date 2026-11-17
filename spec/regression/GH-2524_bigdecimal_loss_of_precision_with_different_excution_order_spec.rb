require 'bigdecimal'

# https://github.com/jruby/jruby/issues/2524
describe 'BigDecimal precision test with different execution order' do
  it 'returns same precision ' do
    fraction = BigDecimal("0.0095") / 365 * BigDecimal(50_000)
    r1 = fraction * BigDecimal(50_000) / BigDecimal(100_000)
    r2 = fraction * (BigDecimal(50_000) / BigDecimal(100_000))
    expect(r1).to eq(r2)
  end
end
