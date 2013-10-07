require 'rspec'

describe 'time add precision' do
  it 'handles BC time correctly' do
    time = Time.new(0)
    (time + -1).should eq(Time.new(-1, 12, 31, 23, 59, 59))
  end
end

describe 'time minus precision' do
  it 'handles BC time correctly' do
    time = Time.new(0)
    other_time = Time.new(2012, 5, 23, 12, 0, 0)
    (time - other_time).should eq(-63504988500.0)
  end
end