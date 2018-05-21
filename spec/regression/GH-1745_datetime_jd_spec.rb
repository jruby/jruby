require 'rspec'

# https://github.com/jruby/jruby/issues/1745
describe 'DateTime#jd' do
  before(:all) { require 'date' }

  it 'returns chronological Julian day number' do
    d1 = DateTime.parse('2014-04-05T23:59:59-07:00')

    expect(d1.jd).to eq(2456753)

    # see example below
    # http://www.ruby-doc.org/stdlib-1.9.3/libdoc/date/rdoc/Date.html#method-i-jd
    expect(DateTime.new(2001,2,3,4,5,6,'+7').jd).to eq(2451944)
    expect(DateTime.new(2001,2,3,4,5,6,'-7').jd).to eq(2451944)
  end
end
