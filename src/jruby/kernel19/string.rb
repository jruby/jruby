class String
  alias_method :old_format, :%
  def %(replacements)
    split_re = /(?<!%)(%{[^}]+})/
    replace_re = /(?<!%)%{([^}]+)}/
    if ! replacements.is_a? Hash
      if split_re.match self
        raise ArgumentError, "one hash required"
      else
        return self.old_format replacements
      end
    end
    segments = self.split split_re
    segments.each_index do |i; md, key|
    md = replace_re.match(segments[i])
    if ! md.nil?
      key = md.captures[0].to_sym
      raise KeyError, "key[#{key}] not found" unless replacements.has_key?(key)
      segments[i] = replacements[key]
    else
      segments[i] = segments[i].gsub "%%", "%"
    end
    end
    segments.join
  end
end

class KeyError < IndexError
end