require 'rspec'

describe 'Enumerable#reject' do
  # https://github.com/jruby/jruby/issues/8252
  it 'breaks up array with , form of proc' do
    held = nil
    [[1,2]].reject {|e,| held = e }
    expect(held).to eq 1
  end
end
