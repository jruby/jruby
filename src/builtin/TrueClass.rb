class TrueClass
  class << self
    undef_method :new
  end

  def &(other)
    other ? true : false
  end

  def |(other)
    true
  end

  def ^(other)
    other ? false : true
  end

  def id
    2
  end

  def to_s
    "true"
  end
end
