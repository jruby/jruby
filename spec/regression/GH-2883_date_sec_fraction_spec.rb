require 'rspec'
require 'date'

# https://github.com/jruby/jruby/issues/2883

if RUBY_VERSION > '1.9'
  describe 'DateTime.iso8601' do
    it 'correctly parses fraction of a second' do
      date = DateTime.iso8601('2014-07-08T17:51:36.013Z')
      date.sec_fraction.should == Rational(13, 1000)
      date.second_fraction.should == Rational(13, 1000)
    end
  end
end