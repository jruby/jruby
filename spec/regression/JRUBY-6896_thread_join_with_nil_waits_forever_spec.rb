require 'rspec'

describe 'Thread#join' do
  it 'waits forever if passed nil for timeout' do
    start = Time.now
    Thread.new {sleep 0.5}.join(nil)
    (Time.now - start).should >= 0.5
  end
end
