# https://github.com/jruby/jruby/issues/2574
describe 'Local variable assignments should not get clobbered' do
  it 'returns the right value' do
    a = 0
    b = [a,a=1]
    expect(b).to eq([0,1])
  end
end

