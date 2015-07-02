# coding: utf-8

# https://github.com/jruby/jruby/issues/3035
describe 'Symbol#==' do
  it 'returns correct value' do
    expect('a'.to_sym).to eq  :'a'
    expect('あ'.to_sym).to eq  :'あ'
  end
end
