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
  end
end
