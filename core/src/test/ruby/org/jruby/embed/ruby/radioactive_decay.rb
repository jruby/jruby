# radioactive_decay.rb [embed]

# Calculates Radioactive decay
# $h: half-life in years
# q0: initial quantity
# q: quantity left after t years
# t: years

def amount_after_years(q0, t)
  q0 * Math.exp(1.0 / $h * Math.log(1.0/2.0) * t)
end

def years_to_amount(q0, q)
  $h * (Math.log(q) - Math.log(q0)) / Math.log(1.0/2.0)
end
