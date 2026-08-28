# next_year_1.rb [embed]

require 'date'

class NextYear
  def initialize
    @today = DateTime.now
  end

  def get_year
    @today.year + 1
  end
end
NextYear.new