# https://github.com/jruby/jruby/issues/1675
if RUBY_VERSION > '1.9'
  describe 'String#casecmp' do
    it 'returns correct value' do
      Encoding.name_list.each do |enc_name|
        enc = Encoding.find(enc_name)
        next if !enc || enc.dummy?
        a = 'ABC'.encode(enc)
        b = 'ABC'.encode(enc)
        b.casecmp(a).should == 0
      end
    end
  end
end
