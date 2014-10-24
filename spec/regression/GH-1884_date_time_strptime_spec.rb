require 'rspec'
require 'date'

# https://github.com/jruby/jruby/issues/1884
if RUBY_VERSION > '1.9'
  describe 'DateTime#strptime' do
    it 'returns correct value' do
      date = "05/19/2014 10:58:59"

      # using %s - Number of seconds since 1970-01-01 00:00:00 UTC.
      value = DateTime.strptime(date, "%m/%d/%Y %H:%M:%s")
      value.to_s.should == '2014-05-19T10:58:00+00:00'

      # using %S - Second of the minute (00..59)
      value = DateTime.strptime(date, "%m/%d/%Y %H:%M:%S")
      value.to_s.should == '2014-05-19T10:58:59+00:00'
    end
  end
end
