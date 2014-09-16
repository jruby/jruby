# https://github.com/jruby/jruby/issues/1969

def yielder; yield; end

def foo; yield 1,2,3,4,5,6,7; end

describe 'Blocks with duplicate _ args' do
  it 'should not crash' do
    foo do |x,_,_,_,a,b,c|
      yielder {}
      [x,_,a,b,c]
    end.should_be [1,2,5,6,7]
  end
end
