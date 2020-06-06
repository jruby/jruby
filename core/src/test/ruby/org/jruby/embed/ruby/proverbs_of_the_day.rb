# proverbs_of_the_day.rb [embed]

@proverbs = [
  "A rolling stone gathers no moss.",
  "A friend in need is a friend indeed.",
  "Every garden may have some weeds.",
  "Fine feathers make fine birds."]

def get_proverb
  if $day + 1 < @proverbs.length
    $day += 1
  else
    $day = 0
  end
  @proverbs[$day]
end
get_proverb