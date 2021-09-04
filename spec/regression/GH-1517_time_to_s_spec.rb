require 'rspec'

# https://github.com/jruby/jruby/issues/1517
describe 'Time#to_s' do
  it 'returns the same string' do
    t1 = Time.new(2014, 1, 2, 3, 4, 5, '+05:00')
    t2 = Time.new(2014, 1, 2, 3, 4, 5, 18000)

    expect(t1.to_s).to eq('2014-01-02 03:04:05 +0500')
    expect(t1.to_s).to eq(t2.to_s)
  end
end

describe 'Time#eql?' do
  it 'returns true' do
    t1 = Time.new(2014, 1, 2, 3, 4, 5, '+05:00')
    t2 = Time.new(2014, 1, 2, 3, 4, 5, 18000)

    expect(t1.eql?(t2)).to eq(true)
  end
end

describe 'Time#zone' do
  it 'returns nil' do
    t1 = Time.new(2014, 1, 2, 3, 4, 5, '+05:00')

    # in ruby 1.9.3, t1.zone return nil
    expect(t1.zone).to eq(nil)
  end
end

describe 'Time#utc_offset' do
  it 'returns collect value' do
    t1 = Time.new(2014, 1, 2, 3, 4, 5, '+05:00')

    # in ruby 1.9.3, t1.utc_offset return 18000
    expect(t1.utc_offset).to eq(18000)
  end
end

describe Time do
  describe '#new' do
    let(:year) { 2014 }
    let(:month) { 5 }
    let(:day) { 21 }
    let(:hour) { 22 }
    let(:minute) { 51 }
    let(:second) { 23 }

    # These two timezone offsets should be equivalent
    let(:offset_int) { -25200 }
    let(:offset_str) { '-07:00' }

    let(:instance_int) { Time.new(year, month, day, hour, minute, second, offset_int) }
    let(:instance_str) { Time.new(year, month, day, hour, minute, second, offset_str) }

    it 'creates equal instances with both UTC offset representations' do
      # Fails on JRuby 1.7.10 with
      #  expected: 2014-05-21 22:51:23 -0700
      #       got: 2014-05-21 08:51:23 -0700
      #
      #  (compared using ==)
      expect(instance_str).to eq(instance_int)
    end
  end
end