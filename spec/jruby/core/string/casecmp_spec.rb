# https://github.com/jruby/jruby/issues/1675
describe 'String#casecmp' do
  it 'returns correct value' do
    Encoding.list.each do |enc|
      next if enc.dummy?

      # using "UTF-16LE", "UTF-8", "Shift_JIS", and other available encodings
      a = 'ABC'.encode(enc)
      b = 'ABC'.encode(enc)
      expect(b.casecmp(a)).to be_truthy
    end
  end
end
