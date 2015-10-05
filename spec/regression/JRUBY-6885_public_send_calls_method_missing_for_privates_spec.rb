require 'rspec'

describe 'Kernel#public_send' do
  it 'invokes method missing when the name in question is defined but not public' do
    obj = Class.new do
def method_missing(name, *)
  name
end
def foo; end
private :foo
def bar; end
protected :bar
    end.new

    expect(obj.public_send(:foo)).to eq(:foo)
    expect(obj.public_send(:bar)).to eq(:bar)
  end
end
