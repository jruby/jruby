require 'bigdecimal'

# https://github.com/jruby/jruby/issues/1633
describe 'BigDecimal#mult' do
  it 'raises a type error exception when the first param is nil' do
    lambda { BigDecimal.new('10').mult(nil, 3) }.should raise_error(TypeError)
  end
end

describe 'BigDecimal#*' do
  it 'raises a type error exception when the first param is nil' do
    lambda { BigDecimal.new('10') * nil }.should raise_error(TypeError)
  end
end

describe 'BigDecimal#div' do
  it 'raises a type error exception when the first param is nil' do
    lambda { BigDecimal.new('10').div(nil, 3) }.should raise_error(TypeError)
  end
end
