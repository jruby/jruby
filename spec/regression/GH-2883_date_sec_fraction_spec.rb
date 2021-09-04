require 'rspec'

describe 'DateTime.iso8601' do

  before(:all) { require 'date' }

  it 'correctly parses fraction of a second' do
    date = DateTime.iso8601('2014-07-08T17:51:36.013Z')
    expect(date.sec_fraction).to eq(Rational(13, 1000))
    expect(date.second_fraction).to eq(Rational(13, 1000))
  end

end