# Windows symlink support borrowed from djberg96/win32-file and ffi-win32-extensions

if JRuby::Util::ON_WINDOWS

  class File
    def self.symlink(target, link)
      # lazy load FFI-based File.symlink logic
      require_relative 'jruby/windows_ffi_file'

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
      # lazy load FFI-based File#flock logic
      require_relative 'jruby/solaris_ffi_file'

      flock(operation)
    end
  end

end
