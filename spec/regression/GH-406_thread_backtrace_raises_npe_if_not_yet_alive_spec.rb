require 'rspec'

describe 'A thread that has not yet started running' do
  it 'should return nil for Thread#backtrace' do
    running = true
    Thread.new do
      1000.times do
        Thread.new {}.join
      end
      running = false
    end

    expect do
      while running
        Thread.list.each &:backtrace
      end
    end.to_not raise_error
  end
end if Thread.instance_methods.include? :backtrace
