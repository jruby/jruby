require 'rspec'

describe 'Time.at with float' do
  it 'preserves sub-ms part when passing through getutc' do
    time = Time.at(1234441536.123456)
    utc = time.getutc
    expect(utc.to_f.to_s).to eq('1234441536.123456')
  end

  it 'preserves sub-ms part when passing through localtime' do
    time = Time.at(1234441536.123456)
    local = time.localtime
    expect(local.to_f.to_s).to eq('1234441536.123456')
  end
end
