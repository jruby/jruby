module JRuby
  module PathHelper
    # This function does a semi-smart split of a space-separated command line.
    # It is aware of single- and double-quotes as word delimiters, and use of backslash
    # as an escape character.
    def self.quote_sensitive_split(cmd)
      parts = [""]
      in_quote = nil
      escape = false
      cmd.each_char do |c|
        if escape
          parts.last << c
          escape = false
        else
          case c
          when "\\"
            escape = true
          when "\"", "'"
            if in_quote && in_quote == c
              in_quote = nil
            elsif in_quote.nil?
              in_quote = c
            else
              parts.last << c
            end
          when " " # space
            if in_quote
              parts.last << c
            else
              parts << "" unless parts.last.empty?
            end
          else
            parts.last << c
          end
        end
      end
      parts
    end

    # Split the command string into parts, but also check if the first argument
    # looks like an absolute path, and if so, try to reconstruct it even in the
    # case where there are spaces in the directory name. It does this by joining
    # successive parts of the front of the split array together until the path
    # exists.
    # NOTE: this is not foolproof, as someone could have a directory and a file:
    #    c:/Program Files/
    #    c:/Program.bat
    # but hopefully good enough!
    def self.smart_split_command(cmd)
      orig_parts = quote_sensitive_split(cmd.strip)
      parts = orig_parts.dup
      exe = parts.shift
      if exe =~ %r{^([a-zA-Z]:)?[/\\]} && !File.exist?(exe)
        until parts.empty? || File.exist?(exe)
          exe << " #{parts.shift}"
        end
        if parts.empty?
          orig_parts
        else
          [exe, *parts]
        end
      else
        orig_parts
      end
    end
  end
end
