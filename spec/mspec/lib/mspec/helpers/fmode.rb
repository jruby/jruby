require 'mspec/guards/feature'

class Object
  # This helper simplifies passing file access modes regardless of
  # whether the :encoding feature is enabled. Only the access specifier
  # itself will be returned if :encoding is not enabled. Otherwise,
  # the full mode string will be returned (i.e. the helper is a no-op).
  def fmode(mode)
    if FeatureGuard.enabled? :encoding
      mode
    else
      mode.split(':').first
    end
  end
end
