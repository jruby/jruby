class BasicObject
  # replace Java versions that don't cache with Ruby versions that do
  def !=(other)
    !(self == other)
  end
end