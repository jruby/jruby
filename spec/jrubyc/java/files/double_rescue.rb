class DoubleRescue
  def _call
    # no-op
  rescue LoadError => e
    e || 1 # doesn't matter
  rescue => e
    e || 2 # doesn't matter
  end
end
