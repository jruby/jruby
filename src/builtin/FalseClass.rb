class FalseClass
  class << self
    undef_method :new
  end

  def &(other)
    false
  end

  def |(other)
    other ? true : false
  end

  def ^(other)
    other ? true : false
  end

  def id
    0
  end

  def to_s
    "false"
  end
end
