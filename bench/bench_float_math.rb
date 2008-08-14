total = 0.0

1.0.step(2000.0,0.0001) do |x|

  result = (5.4*x**5 - 3.211*x**4 + 100.3*x**2 - 100 +
    20*Math.sin(x) - Math.log(x)) * 20*Math.exp(-x/100.3)
  total += result / 0.0001

end

puts total
