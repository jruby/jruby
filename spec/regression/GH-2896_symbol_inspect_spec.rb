# encoding: utf-8

# https://github.com/jruby/jruby/issues/2896
if RUBY_VERSION > '1.9'
  describe 'Symbol#inspect' do
    it 'returns correct value' do
      :"Ãa1".inspect.should == ":Ãa1"
      :"a1".inspect.should == ":a1"
      :"1".inspect.should == ":\"1\""
    end
  end
end
