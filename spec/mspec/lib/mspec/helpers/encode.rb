require 'mspec/guards/feature'

class Object
  # Helper to handle String encodings. The +str+ and +encoding+ parameters
  # must be Strings and an ArgumentError will be raised if not. This ensures
  # that the encode() helper can be used regardless of whether Encoding exits.
  # The helper is a no-op (i.e. passes through +str+ unmodified) if the
  # :encoding feature is not enabled (see with_feature guard).  If the
  # :encoding feature is enabled, +str+.force_encoding(+encoding+) is called.
  def encode(str, encoding)
    unless str.is_a? String and encoding.is_a? String
      raise ArgumentError, "encoding name must be a String"
    end

    if FeatureGuard.enabled? :encoding
      str.force_encoding encoding
    end

    str
  end
end
