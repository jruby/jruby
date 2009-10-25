# next_year.rb [embed]

require 'date'

class NextYear
  def initialize
    @today = DateTime.now
  end

  def get_year
    @today.year + 1
  end
end
NextYear.new.get_year