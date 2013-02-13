require 'rspec'

describe 'Enumerator#find_index' do
  it 'should pass the correct number of arguments to the block' do
    Enumerator.new { |y| y.yield :success }.find_index { |e| e == :success }.should == 0
  end
end
