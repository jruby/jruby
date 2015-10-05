require 'rspec'

describe 'JRUBY-2388: GC methods' do
  it 'do not appear on other classes' do
    expect(Module.respond_to?(:enable)).to eq(false)
    expect(Kernel.respond_to?(:start)).to eq(false)
    expect(String.respond_to?(:enable)).to eq(false)
  end
end
