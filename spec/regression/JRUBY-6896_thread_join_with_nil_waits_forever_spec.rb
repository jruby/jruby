require 'rspec'

describe 'Thread#join' do
  it 'waits forever if passed nil for timeout' do
    start = Time.now
    Thread.new {sleep 0.5}.join(nil)
    expect(Time.now - start).to be >= 0.5
  end
end
