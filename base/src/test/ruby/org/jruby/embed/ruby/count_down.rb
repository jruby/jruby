# count_down.rb [jruby-embed]
# jsr223.JRubyEngineTest

require 'date'

def count_down_birthday
  now = DateTime.now
  year = now.year
  days = DateTime.new(year, @month, @day).yday - now.yday
  if days < 0
    this_year = DateTime.new(year, 12, 31).yday - now.yday
    next_year = DateTime.new(year + 1, @month, @day).yday
    days = this_year + next_year
  end
  return "Happy Birthday!" if days == 0
  return "You have #{days} day(s) to your next birthday!"
end
count_down_birthday