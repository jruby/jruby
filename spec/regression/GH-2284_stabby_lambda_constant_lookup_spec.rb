require 'rspec'

class C
  class D; end
end

describe 'Constant lookup in stabby lambdas' do
  it 'should not crash' do
    -> { C::D.new }.should_not raise_error
  end
end
