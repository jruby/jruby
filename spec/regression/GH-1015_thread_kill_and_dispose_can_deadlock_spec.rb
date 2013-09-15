require 'rspec'

describe 'A thread dying naturally while being killed' do
  it 'should not deadlock' do
    ary = []
    n = 100

    # This logic is crafted to attempt to maximize likelihood of deadlock.
    # It could probably be better.
    n.times do
      running = false
      t = Thread.new do
        running = true
      end
      Thread.pass until running
      t.kill
      t.join
      ary << :ok
    end

    ary.should == [:ok] * n
  end
end
