require 'rspec'
require 'bigdecimal'

describe 'BigDecimal' do
  it 'should be duplicable' do
    a = BigDecimal.new(1)

    expect(a.dup).to eq(a)
  end
end
