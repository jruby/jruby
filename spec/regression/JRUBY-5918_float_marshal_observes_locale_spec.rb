require 'java'

describe 'JRUBY-5918: Float marshaling' do
  it 'always uses . for decimal separator, regardless of locale' do
    old = java.util.Locale.default
    begin
      java.util.Locale.default = java.util.Locale::FRENCH
      expect(Marshal.dump(1.3)).to eq("\004\bf\b1.3")
      expect(Marshal.load(Marshal.dump(1.3))).to eq(1.3)
    ensure
      java.util.Locale.default = old
    end
  end
end
