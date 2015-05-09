# https://github.com/jruby/jruby/issues/1675
if RUBY_VERSION > '1.9'
  describe 'String#casecmp' do
    it 'returns correct value' do
      Encoding.list.each do |enc|
        next if enc.dummy?

        # using "UTF-16LE", "UTF-8", "Shift_JIS", and other available encodings
        a = 'ABC'.encode(enc)
        b = 'ABC'.encode(enc)
        b.casecmp(a).should be_true
      end
    end
  end
end
