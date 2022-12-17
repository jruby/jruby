# -*- encoding: utf-8 -*-

# https://github.com/jruby/jruby/issues/7529
if RUBY_VERSION > '1.9'
  describe 'Enumerator#inspect' do
    it 'returns UTF_8 String value when needed' do
      e = ["ΆἅἇἈ"].each
      expect(e.inspect).to eq("#<Enumerator: [\"ΆἅἇἈ\"]:each>")
      expect(e.inspect.encoding).to eq(Encoding::UTF_8)
    end
    it 'returns ASCII_8BIT String value when possible' do
      e = ["abc"].each
      expect(e.inspect).to eq("#<Enumerator: [\"abc\"]:each>")
      expect(e.inspect.encoding).to eq(Encoding::ASCII_8BIT)
    end
    it 'returns UTF_8 String value when the method is UTF_8' do
      # example derived from TestEnumerator#test_inspect_encoding
      # in test/mri/ruby/test_enumerator.rb
      c = Class.new{define_method(:"\u{3042}"){}}
      e = c.new.enum_for(:"\u{3042}")
      expect(e.inspect).to match(/\A#<Enumerator: .*:\u{3042}>\z/)
      expect(e.inspect.encoding).to eq(Encoding::UTF_8)
    end
  end
end
