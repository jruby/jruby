require 'rspec'

describe 'Array#each_slice' do
  describe 'when #transpose is invoked in the block' do
    it 'does not raise error' do
      lambda {
        [[1],[2]].each_slice(1) { |slice| slice.transpose }
      }.should_not raise_error
    end
  end
end