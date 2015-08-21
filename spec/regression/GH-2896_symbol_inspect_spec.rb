# encoding: utf-8

# https://github.com/jruby/jruby/issues/2896
if RUBY_VERSION > '1.9'
  describe 'Symbol#inspect' do
    it 'returns correct value' do
      expect(:"Ãa1".inspect).to eq(":Ãa1")
      expect(:"a1".inspect).to eq(":a1")
      expect(:"1".inspect).to eq(":\"1\"")
    end
  end
end
