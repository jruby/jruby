require 'bigdecimal'

# https://github.com/jruby/jruby/issues/2524
describe 'BigDecimal precision test with different execution order' do
  it 'returns same precision ' do
    fraction = BigDecimal.new("0.0095") / 365 * BigDecimal.new(50_000)
    r1 = fraction * BigDecimal.new(50_000) / BigDecimal.new(100_000)
    r2 = fraction * (BigDecimal.new(50_000) / BigDecimal.new(100_000))
    expect(r1).to eq(r2)
  end
end
