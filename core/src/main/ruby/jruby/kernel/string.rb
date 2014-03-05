class String
  # Removes invalid byte sequences from a String, available since Ruby 2.1.
  def scrub(replace = nil)
    new_str = ''

    # The default replacement character is the "Unicode replacement" character.
    # (UFFFD).
    if !replace and !block_given?
      replace = "\xEF\xBF\xBD".force_encoding("UTF-8").encode(self.encoding, :undef => :replace, :replace => '?')
    end

    if replace and !replace.is_a?(String)
      raise(
          TypeError,
          "no implicit conversion of #{replace.class} into String"
      )
    end

    if replace and !replace.valid_encoding?
      raise(
          ArgumentError,
          "replacement must be a valid byte sequence '#{replace.inspect}'"
      )
    end

    chars.each do |char|
      if char.valid_encoding?
        new_str << char
      else
        if block_given?
          new_str << yield(char)
        else
          new_str << replace
        end
      end
    end

    return new_str
  end

end