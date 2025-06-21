require 'rspec'

# https://github.com/jruby/jruby/issues/8854
describe 'Time#new with empty keywords' do
  it 'should not raise NullPointerException' do
    expect(Time.new(2000, 3, 2, **{})).to eq Time.new(2000, 3, 2)
  end
end
