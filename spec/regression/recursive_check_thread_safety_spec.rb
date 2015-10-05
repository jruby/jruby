require 'rspec'

describe 'Joining an array that contains arrays' do
  it 'should work across multiple threads without error' do
    a = [(1..100).to_a]
    expect do
      result = (1..100).map { Thread.new { 10.times { a.join }; :ok } }.map(&:value)
      expect(result).to eq([:ok] * 100)
    end.not_to raise_exception
  end
end
