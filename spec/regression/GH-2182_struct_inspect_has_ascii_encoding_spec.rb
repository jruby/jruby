# -*- encoding: utf-8 -*-

# https://github.com/jruby/jruby/issues/2182
if RUBY_VERSION > '1.9'
  describe 'Struct#inspect' do
    it 'returns correct value' do
      s1 = Struct.new(:aa).new("ΆἅἇἈ")
      expect(s1.inspect).to eq("#<struct aa=\"ΆἅἇἈ\">")
      expect(s1.inspect.encoding).to eq(Encoding::UTF_8)

      s2 = Struct.new(:a, :b).new("ΆἅἇἈ", "abc")
      expect(s2.inspect).to eq("#<struct a=\"ΆἅἇἈ\", b=\"abc\">")
      expect(s2.inspect.encoding).to eq(Encoding::UTF_8)

      s3 = Struct.new(:b).new("abc")
      expect(s3.inspect).to eq("#<struct b=\"abc\">")
      expect(s3.inspect.encoding).to eq(Encoding::ASCII_8BIT)

      s4 = Struct.new(:"ΆἅἇἈ").new("aa")
      expect(s4.inspect).to eq("#<struct ΆἅἇἈ=\"aa\">")
      expect(s4.inspect.encoding).to eq(Encoding::UTF_8)
    end
  end
end
