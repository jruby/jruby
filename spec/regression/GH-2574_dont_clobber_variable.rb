# https://github.com/jruby/jruby/issues/2574
describe 'Local variable assignments should not get clobbered' do
  it 'returns the right value for array literals' do
    a = 0
    b = [a,a=1]
    expect(b).to eq([0,1])
  end

  it 'returns the right value for hash literals' do
    a = 0
    b = { a => a, (a = 1) => a } # => { 1 => 1 } (MRI: {0=>0, 1=>1})
    c = { a => a, a => (a = 2) } # => { 2 => 2 } (MRI: {1=>2})
    expect(b).to eq({0=>0, 1=>1})
    expect(c).to eq({1=>2})
  end
end

