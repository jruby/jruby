require 'rspec'

describe 'Time#+' do
  it 'does not introduce rounding errors' do
    time = Time.new(2012, 10, 17, 23, 59, 59, 86399.9)
    time_p1 = time + 1
    time_p1_m1 = time_p1 - 1

    expect(time_p1.to_i - time.to_i).to eq(1)
    expect(time_p1_m1.to_i).to eq(time.to_i)
  end
end
