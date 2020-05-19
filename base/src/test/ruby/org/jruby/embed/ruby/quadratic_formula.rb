# quadratic_formula.rb [embed]

# if ax^2+bx+c=0 and b^2-4ac >=0 then
# x = (-b +/- sqrt(b^2-4ac))/2a

def solve(a, b, c)
  v = b ** 2 - 4 * a * c
  if v < 0; raise RangeError end
  s0 = ((-1)*b - Math.sqrt(v))/(2*a)
  s1 = ((-1)*b + Math.sqrt(v))/(2*a)
  return s0, s1
end