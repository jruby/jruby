#!/usr/bin/ruby
# The Computer Language Benchmark Shootout
# http://shootout.alioth.debian.org
#
# original code by Martin DeMello
# modified by Jabari Zakiya 3/20/05
# modified by Glenn Parker 3/28/05 (format results)

include Math

SOLAR_MASS = 4*PI*PI
DAYS_PER_YEAR = 365.24

Vector3D = Struct.new("Vector3D", :x, :y, :z)

class Vector3D

  def *(val)
    Vector3D.new(*self.map {|i| i * val})
  end

  def /(val)
    Vector3D.new(*self.map {|i| i / val})
  end

  #in-place add with scale
  # a.adds(b, s) -> a += b*s

  def adds(other, scale)
    self[0] += other[0]*scale; self[1] += other[1]*scale
    self[2] += other[2]*scale
  end

  def subs(other, scale)
    self[0] -= other[0]*scale; self[1] -= other[1]*scale
    self[2] -= other[2]*scale
  end

  def magnitude
    x=self[0]; y=self[1]; z=self[2]
    sqrt(x*x + y*y + z*z)
  end

  # |self - other|
  def dmag(other)
    x=self[0]-other[0]; y=self[1]-other[1]; z=self[2]-other[2]
    sqrt(x*x + y*y + z*z)
  end
end

class Planet
  attr_accessor :pos, :v, :mass

  def initialize(x, y, z, vx, vy, vz, mass)
    @pos = Vector3D.new(x, y, z)
    @v = Vector3D.new(vx, vy, vz) * DAYS_PER_YEAR
    @mass = mass * SOLAR_MASS
  end

  def distance(other)
    self.pos.dmag(other.pos)
  end
end

jupiter = Planet.new(
   4.84143144246472090e+00,
   -1.16032004402742839e+00,
   -1.03622044471123109e-01,
   1.66007664274403694e-03,
   7.69901118419740425e-03,
   -6.90460016972063023e-05,
   9.54791938424326609e-04)

saturn = Planet.new(
   8.34336671824457987e+00,
   4.12479856412430479e+00,
   -4.03523417114321381e-01,
   -2.76742510726862411e-03,
   4.99852801234917238e-03,
   2.30417297573763929e-05,
   2.85885980666130812e-04)

uranus = Planet.new(
   1.28943695621391310e+01,
   -1.51111514016986312e+01,
   -2.23307578892655734e-01,
   2.96460137564761618e-03,
   2.37847173959480950e-03,
   -2.96589568540237556e-05,
   4.36624404335156298e-05)

neptune = Planet.new(
   1.53796971148509165e+01,
   -2.59193146099879641e+01,
   1.79258772950371181e-01,
   2.68067772490389322e-03,
   1.62824170038242295e-03,
   -9.51592254519715870e-05,
   5.15138902046611451e-05)

sun = Planet.new(0, 0, 0, 0, 0, 0, 1)

class Array
  def each_pair
    a = []
    each_index {|i|
      ((i+1)...length).each {|j|
        yield at(i), at(j)
      }
    }
  end
end

bodies = [sun, jupiter, saturn, uranus, neptune]

class << bodies
  def advance(dt)
    mag = m1 = m2 = nil
    each_pair {|b1, b2|
      d = b1.distance(b2)
      mag = dt/(d*d*d)

      m1 = b1.mass * mag
      m2 = b2.mass * mag

      b1.v.adds(b2.pos, m2)
      b1.v.subs(b1.pos, m2)
      b2.v.adds(b1.pos, m1)
      b2.v.subs(b2.pos, m1)
    }

    each {|b| b.pos.adds(b.v, dt)}
  end

  def energy
    e = 0
    each {|b| e += 0.5 * b.mass * (b.v.magnitude ** 2) }
    each_pair {|b1, b2| e -= (b1.mass * b2.mass) / b1.distance(b2) }
    e
  end

  def offset_momentum
    p = Vector3D.new(0,0,0)
    sun = self[0]
    each {|b| p.adds(b.v, b.mass) }
    sun.v.subs(p, 1.0/sun.mass)
  end
end

bodies.offset_momentum
puts "%.9f" % bodies.energy
Integer(ARGV[0]).times { bodies.advance(0.01) }
puts "%.9f" % bodies.energy