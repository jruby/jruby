# https://github.com/jruby/jruby/issues/1675
if RUBY_VERSION > '1.9'
  describe 'String#casecmp' do
    it 'returns correct value' do
      Encoding.name_list.each do |enc_name|
        if (enc_name != "UTF-7") && (enc_name != "CP65000")
          # this condition statement escape the following error:
          # Encoding::ConverterNotFoundError: 
          # code converter not found for UTF-7

          # using "UTF-16LE", "UTF-8", "Shift_JIS", and other available encodings
          a = 'ABC'.encode(enc_name)
          b = 'ABC'.encode(enc_name)
          b.casecmp(a).should be_true
        end
      end
    end
  end
end
