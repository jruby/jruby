# https://github.com/jruby/jruby/issues/1969

describe 'Blocks with duplicate _ args' do
  it 'should not crash' do
    o = Object.new
    def o.foo; yield 1,2,3,4,5,6,7; end
    def o.bar; yield; end
    expect(o.foo do |x,_,_,_,a,b,c|
      o.bar {}
      [x,_,a,b,c]
    end).to eq([1,2,5,6,7])
  end
end
