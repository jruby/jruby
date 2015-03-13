# -*- encoding: utf-8 -*-

# https://github.com/jruby/jruby/issues/2182
if RUBY_VERSION > '1.9'
  describe 'Struct#inspect' do
    it 'returns correct value' do
      s1 = Struct.new(:aa).new("ΆἅἇἈ")
      s1.inspect.should == "#<struct aa=\"ΆἅἇἈ\">"
      s1.inspect.encoding.should == Encoding::UTF_8

      s2 = Struct.new(:a, :b).new("ΆἅἇἈ", "abc")
      s2.inspect.should == "#<struct a=\"ΆἅἇἈ\", b=\"abc\">"
      s2.inspect.encoding.should == Encoding::UTF_8

      s3 = Struct.new(:b).new("abc")
      s3.inspect.should == "#<struct b=\"abc\">"
      s3.inspect.encoding.should == Encoding::ASCII_8BIT

      s4 = Struct.new(:"ΆἅἇἈ").new("aa")
      s4.inspect.should == "#<struct ΆἅἇἈ=\"aa\">"
      s4.inspect.encoding.should == Encoding::UTF_8
    end
  end
end
