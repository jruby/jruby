# position_function.rb [embed]

class PositionFunction
  include Java::org.jruby.embed.PositionFunction
  attr :v0, :s0
  def initialize(v0, s0, system)
    @v0 = v0
    @s0 = s0
    if "english" == system.downcase
      @g = -32.0
      @unit_v = "ft./sec"
      @unit_s = "ft."
    end
    if "metric" == system.downcase
      @g = -9.8
      @unit_v = "m/sec"
      @unit_s = "m"
    end
  end

  def get_position(t)
    1.0 / 2.0 * @g * t ** 2.0 + @v0 * t + @s0
  end

  def get_velocity(t)
    @g * t + @v0
  end

  def get_units
    return @unit_v, @unit_s
  end
end
PositionFunction.new(initial_velocity, initial_height, system)
