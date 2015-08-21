require 'rspec'

describe 'Array#each_slice' do
  describe 'when #transpose is invoked in the block' do
    it 'does not raise error' do
      expect {
        [[1],[2]].each_slice(1) { |slice| slice.transpose }
      }.not_to raise_error
    end
  end
end