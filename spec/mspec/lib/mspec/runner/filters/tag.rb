require 'mspec/runner/filters/match'

class TagFilter
  def initialize(what, *tags)
    @what = what
    @tags = tags
  end

  def load
    desc = MSpec.read_tags(@tags).map { |t| t.description }

    @filter = MatchFilter.new(@what, *desc)
    @filter.register
  end

  def unload
    @filter.unregister if @filter
  end

  def register
    MSpec.register :load, self
    MSpec.register :unload, self
  end

  def unregister
    MSpec.unregister :load, self
    MSpec.unregister :unload, self
  end
end
