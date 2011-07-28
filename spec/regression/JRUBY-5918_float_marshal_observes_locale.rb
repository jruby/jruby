require 'java'

describe 'JRUBY-5918: Float marshaling' do
  it 'always uses . for decimal separator, regardless of locale' do
    old = java.util.Locale.default
    begin
      java.util.Locale.default = java.util.Locale::FRENCH
      Marshal.dump(1.3).should == "\004\bf\b1.3"
      Marshal.load(Marshal.dump(1.3)).should == 1.3
    ensure
      java.util.Locale.default = old
    end
  end
end
