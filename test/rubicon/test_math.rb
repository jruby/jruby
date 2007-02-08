require 'test/unit'


class TestMath < Test::Unit::TestCase

  #
  # Check a float for approximate equality
  #
  # FIXME: nuke this or move it to a common location.
  def assert_flequal(exp, actual, msg=nil)
    assert_in_delta(exp, actual, exp == 0.0 ? 1e-7 : exp.abs/1e7, msg)
  end

  def test_constants
    assert_flequal(3.141592654, Math::PI)
    assert_flequal(2.718281828, Math::E)
  end

  # We're not testing the accuracy here, as much as performing some
  # sanity checks

  def test_fns
    pi   = Math::PI
    pi_2 = pi / 2.0       # 90 degrees
    pi_3 = pi / 3.0       # 60
    pi_4 = pi / 4.0       # 45
    pi_6 = pi / 6.0       # 30

    ten_pi = Math::PI * 10.0

    tests =  {
      :cos => 
      [ [ 0.0,      1.0 ],
        [ pi_2,     0.0 ],
        [ pi_3,     0.5 ],
        [ pi_4,     0.707106781 ],
        [ pi_6,     0.866025402 ],
        [ pi,      -1.0 ],
        [ 3*pi_2,   0.0 ],
        [ ten_pi,   1.0 ],
        [ -pi_2,    0.0 ],
        [ -pi,     -1.0 ],
      ],
      :exp =>
      [ [ -1.0,    0.367879441], 
        [ -0.5,    0.606530659],
        [  0.0,    1.0],
        [  1.0,    Math::E],
        [  5.5,    244.6919323],
      ],
      :frexp =>
      [ [ 0.0,     [ 0.0, 0 ]],
        [ 1.0,     [ 0.5, 1 ]],
        [ 2.0,     [ 0.5, 2 ]],
        [ 8.0,     [ 0.5, 4 ]],
        [ 1.3,     [ 0.65, 1 ]],
        [-1.3,     [-0.65, 1 ]],
      ],
      :log =>
      [ [ 0.5,     -0.69314718],
        [ 1.0,      0.0 ],
        [ Math::E,  1.0 ],
        [ 100,      4.605170186 ],
      ],
      :log10 =>
      [ [ 0.5,     -0.301029995],
        [ 1.0,      0.0 ],
        [ Math::E,  0.434294481 ],
        [ 100,      2.0 ],
      ],
      :sin =>
      [ [ 0.0,      0.0 ],
        [ pi_2,     1.0 ],
        [ pi_6,     0.5 ],
        [ pi_4,     0.707106781 ],
        [ pi_3,     0.866025402 ],
        [ pi,       0.0 ],
        [ 3*pi_2,  -1.0 ],
        [ ten_pi,   0.0 ],
        [ -pi_2,   -1.0 ],
        [ -pi,      0.0 ],
      ],
      :sqrt =>
      [ [ 0.0,      0.0],
        [ 1.0,      1.0],
        [ 2.0,      1.414213562 ],
        [ 100,      10 ],
      ],
      :tan =>
      [ [ 0.0,      0.0 ],
        [ pi_6,     0.577350269 ],
        [ pi_4,     1.0 ],
        [ pi_3,     1.732050808 ],
        [-pi_3,    -1.732050808 ],
      ]
    }

    tests.each do |fn, testlist|
      testlist.each do | angle, expected |
        got = Math.send(fn, angle)
        if expected.instance_of? Float
          assert_flequal(expected, got, "Math.#{fn}(#{angle})")
        else
          assert_equal(expected, got, "Math.#{fn}(#{angle})")
        end
      end
    end

    assert_flequal(0.0,     Math.ldexp(0.0, 20))
    assert_flequal(20.0,    Math.ldexp(20.0, 0))
    assert_flequal(41.0,    Math.ldexp(20.5, 1))
    assert_flequal(1259.52, Math.ldexp(1.23, 10))
    assert_flequal(0.25,    Math.ldexp(1, -2))

    res = [-2.35619449019234, -0.785398163397448,
            2.35619449019234,  0.785398163397448 ]

    for x in [ -0.5, 0.5]
      for y in [-0.5, 0.5]
        assert_flequal(res.shift, Math.atan2(x, y))
      end
    end

    # and some special cases
    assert_equal(-1, Math.log(0).infinite?)
    assert_equal(-1, Math.log10(0).infinite?)
  end
end
