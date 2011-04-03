require 'rspec'

describe 'JRUBY-2388: GC methods' do
  it 'do not appear on other classes' do
    Module.respond_to?(:enable).should == false
    Kernel.respond_to?(:start).should == false
    String.respond_to?(:enable).should == false
  end
end
