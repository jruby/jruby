require 'rspec'

describe 'mod rescue' do
  it 'can capture $! and return backtrace_locations without crashing' do
    exception = (1/0) rescue $!
    expect(exception.backtrace_locations).not_to be_nil
  end
end
