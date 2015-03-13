# https://github.com/jruby/jruby/issues/2574
describe 'Local variable assignments should not get clobbered and returns the' do
  it 'right value for array literals' do
    a = 0
    b = [a,a=1]
    expect(b).to eq([0,1])
  end

  it 'right value for hash literals' do
    a = 0
    b = { a => a, (a = 1) => a }
    c = { a => a, a => (a = 2) }
    expect(b).to eq({0=>0, 1=>1})
    expect(c).to eq({1=>2})
  end

  it 'right value for argspush' do
    a = 0
    b = [a, *a=1]
    expect(b).to eq([0, 1])
  end

  it 'right value for reassigned arg in a call' do
    def foo(*r); r; end
    a = 0
    expect(foo(a, a=1)).to eq([0,1])
  end

  it 'right value for reassigned receiver in a call' do
    a = 10
    b = a / (a = 5)
    expect(b).to eq(2)
  end

  it 'right value for reassigned arg in attrasgn' do
    x = [1,2]
    x[a=0] = a=3
    expect(x).to eq([3,2])
  end

  it 'right value for reassigned receiver in attrasgn' do
    x = [1,2]
    x[x=1] = 5
    expect(x).to eq(1)
  end
end

