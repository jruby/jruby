# Windows symlink support borrowed from djberg96/win32-file and ffi-win32-extensions

if JRuby::Util::ON_WINDOWS

  class File
    def self.symlink(target, link)
      # lazy load FFI-based file logic
      require_relative 'jruby/file_ffi'

      symlink(target, link)
    end
  end

  class String
    # Convenience method for converting strings to UTF-16LE for wide character
    # functions that require it.
    #
    def wincode
      (self.tr(File::SEPARATOR, File::ALT_SEPARATOR) + 0.chr).encode('UTF-16LE')
    end
  end

end

# flock support for Solaris
if JRuby::Util::ON_SOLARIS

  class File
    def flock(operation)
      # lazy load FFI-based file logic
      require_relative 'jruby/file_ffi'

      flock(operation)
    end
  end

end
