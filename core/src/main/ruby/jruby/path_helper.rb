require 'rbconfig'

module JRuby
  module PathHelper

    WINDOWS = RbConfig::CONFIG['host_os'] =~ /mswin/
    WINDOWS_EXE_SUFFIXES = [".exe", ".com", ".bat" , ".cmd"]

    # This function does a semi-smart split of a space-separated command line.
    # It is aware of single- and double-quotes as word delimiters, and use of backslash
    # as an escape character.
    def self.quote_sensitive_split(cmd)
      parts = [""]
      in_quote = nil
      escape = false
      # this needed to handle weird cases of empty quoted strings,
      # like jrubyc -p "" blah.rb
      were_quotes = false
      cmd.each_char do |c|
        if escape
          # escaped quote -- add as is
          if (c == "\"" || c == "'")
            parts.last << c
          else
            # bare slash, not escaping quotes, add it back
            parts.last << "\\" << c
          end
          escape = false
        else
          case c
          when "\\"
            escape = true
          when "\"", "'"
            if in_quote && in_quote == c
              in_quote = nil
              were_quotes = true
            elsif in_quote.nil?
              in_quote = c
            else
              parts.last << c
            end
          when " " # space
            if in_quote
              parts.last << c
            else
              if (!parts.last.empty?)
                parts << ""
              elsif (were_quotes)
                if (parts.last.empty?)
                  # to workaround issue with launching jrubyc -p "",
                  # or java launching code would eat "".
                  parts.last << '""'
                end
                parts << ""
              end
              were_quotes = false
            end
          else
            parts.last << c
          end
        end
      end
      if (were_quotes && parts.last.empty?)
          parts.last << '""'
      end
      parts
    end

    def self.find_file(exe)
      # puts "FindFile: looking for #{exe}"
      if (WINDOWS && exe !~ /\.(exe|com|cmd|bat)$/i)
        WINDOWS_EXE_SUFFIXES.each do |sfx|
          if find_file(exe + sfx)
            return true
          end
        end
      end
      # TODO: should we find files like 'foo' on Windows,
      # or should we only deal with .EXE/.BAT, etc.?
      
      File.exist?(exe) && !File.directory?(exe)
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
      exe = parts.shift.dup
      if exe =~ %r{^([a-zA-Z]:)?[/\\]} && (!(found = find_file(exe)))
        until (found)
          break if parts.empty?
          exe << " #{parts.shift}"
          found = find_file(exe)
        end
        if found
          [exe, *parts]
        else
          orig_parts
        end
      else
        orig_parts
      end
    end
  end
end
