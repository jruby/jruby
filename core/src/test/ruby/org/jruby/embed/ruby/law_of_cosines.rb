# law_of_cosines.rb [embed]
# @a, @b, @c are three sides of a triagle

def angles
  angle1 = angle @a, @b, @c
  angle2 = angle @c, @b, @a
  angle3 = 180.0 - angle1 - angle2
  return angle1, angle2, angle3
end

def angle(a, b, c)
  cosine_theta = abc_to_cosinetheta(a, b, c)
  theta = Math.acos(cosine_theta)
  return radians_to_degrees(theta)
end

def abc_to_cosinetheta(a, b, c)
  nominator = a ** 2.0 + b ** 2.0 - c ** 2.0
  return nominator / (2.0 * a * b)
end

def radians_to_degrees(x)
  return x * 180.0 / Math::PI
end
angles
