require 'bigdecimal'

describe '#4463 BigDecimal marshal' do
  it 'roundtrips correctly' do
    ['1.23', 'Infinity', '+Infinity', '-Infinity'].each do |s|
      a = BigDecimal.new(s)
      b = Marshal.load(Marshal.dump(a))
    
      expect(b).to eq(a)
    end

    nan = BigDecimal.new('NaN')
    expect(Marshal.load(Marshal.dump(nan))).to be_nan
  end
end
