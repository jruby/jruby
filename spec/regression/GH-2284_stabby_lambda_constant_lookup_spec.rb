require 'rspec'

class C
  class D; end
end

describe 'Constant lookup in stabby lambdas' do
  it 'should not crash' do
    expect { C::D.new }.not_to raise_error
  end
end
