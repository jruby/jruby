require 'rspec'

describe 'time add precision' do
  it 'handles BC time correctly' do
    time = Time.new(0, nil, nil, nil, nil, nil, '-06:00')
    expect(time + -1).to eq(Time.new(-1, 12, 31, 23, 59, 59, '-06:00'))
  end
end

describe 'time minus precision' do
  it 'handles BC time correctly' do
    time = Time.new(0, nil, nil, nil, nil, nil, '-06:00')
    other_time = Time.new(2012, 5, 23, 12, 0, 0, '-06:00')
    expect(time - other_time).to eq(-63504993600.0)
  end
end