class MatchFilter
  def initialize(what, *strings)
    @what = what
    @descriptions = to_regexp(*strings)
  end

  def to_regexp(*strings)
    strings.map { |str| Regexp.new Regexp.escape(str) }
  end

  def ===(string)
    @descriptions.any? { |d| d === string }
  end

  def register
    MSpec.register @what, self
  end

  def unregister
    MSpec.unregister @what, self
  end
end
