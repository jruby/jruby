# Windows symlink support borrowed from djberg96/win32-file and ffi-win32-extensions

begin
  require 'ffi'

  module JRuby::Windows
    module File
      module Constants
        FILE_ATTRIBUTE_REPARSE_POINT = 0x00000400
        INVALID_HANDLE_VALUE = (1 << FFI::Platform::ADDRESS_SIZE) - 1
        INVALID_FILE_ATTRIBUTES = (1 << FFI::Platform::ADDRESS_SIZE) - 1
        IO_REPARSE_TAG_SYMLINK = 0xA000000C
      end

      module Functions
        extend FFI::Library
        ffi_lib :kernel32

        typedef :ulong, :dword
        typedef :uintptr_t, :handle
        typedef :pointer, :ptr

        def self.attach_pfunc(*args)
          attach_function(*args)
          private args[0]
        end

        attach_pfunc :CloseHandle, [:handle], :bool
        attach_pfunc :FindFirstFileW, [:buffer_in, :pointer], :handle
        attach_pfunc :GetFileAttributesW, [:buffer_in], :dword

        attach_pfunc :CreateFileW, [:buffer_in, :dword, :dword, :pointer, :dword, :dword, :handle], :handle
        attach_pfunc :GetDiskFreeSpaceW, [:buffer_in, :pointer, :pointer, :pointer, :pointer], :bool
        attach_pfunc :GetDriveTypeW, [:buffer_in], :uint
        attach_pfunc :GetFileType, [:handle], :dword
        attach_pfunc :GetFinalPathNameByHandleW, [:handle, :buffer_out, :dword, :dword], :dword
        attach_pfunc :GetShortPathNameW, [:buffer_in, :buffer_out, :dword], :dword
        attach_pfunc :GetLongPathNameW, [:buffer_in, :buffer_out, :dword], :dword
        attach_pfunc :QueryDosDeviceA, [:string, :buffer_out, :dword], :dword
        attach_pfunc :SetFileTime, [:handle, :ptr, :ptr, :ptr], :bool
        attach_pfunc :SystemTimeToFileTime, [:ptr, :ptr], :bool

        ffi_lib :shlwapi

        attach_pfunc :PathFindExtensionW, [:buffer_in], :pointer
        attach_pfunc :PathIsRootW, [:buffer_in], :bool
        attach_pfunc :PathStripPathW, [:pointer], :void
        attach_pfunc :PathRemoveBackslashW, [:buffer_in], :string
        attach_pfunc :PathRemoveFileSpecW, [:pointer], :bool
        attach_pfunc :PathRemoveExtensionW, [:buffer_in], :void
        attach_pfunc :PathStripToRootW, [:buffer_in], :bool

        ffi_lib :kernel32

        # If this fails to bind we fall back on a NotImplemented symlink method below (jruby/jruby#3998)
        attach_pfunc :CreateSymbolicLinkW, [:buffer_in, :buffer_in, :dword], :bool
      end

      module Structs
        class FILETIME < FFI::Struct
          layout(:dwLowDateTime, :ulong, :dwHighDateTime, :ulong)
        end

        class WIN32_FIND_DATA < FFI::Struct
          layout(
            :dwFileAttributes, :ulong,
            :ftCreationTime, FILETIME,
            :ftLastAccessTime, FILETIME,
            :ftLastWriteTime, FILETIME,
            :nFileSizeHigh, :ulong,
            :nFileSizeLow, :ulong,
            :dwReserved0, :ulong,
            :dwReserved1, :ulong,
            :cFileName, [:uint8, 260 * 2],
            :cAlternateFileName, [:uint8, 14 * 2]
          )
        end
      end
    end
  end

  class ::File
    include JRuby::Windows::File::Constants
    include JRuby::Windows::File::Structs
    extend JRuby::Windows::File::Functions

    # Creates a symbolic link called +new_name+ for the file or directory
    # +old_name+.
    #
    # This method requires Windows Vista or later to work. Otherwise, it
    # returns nil as per MRI.
    #
    def self.symlink(target, link)
      target = string_check(target)
      link = string_check(link)

      flags = File.directory?(target) ? 1 : 0

      wlink = link.wincode
      wtarget = target.wincode

      unless CreateSymbolicLinkW(wlink, wtarget, flags)
        errno = FFI.errno
        # FIXME: in MRI all win calling methods call into a large map between windows errors and unixy ones.  We
        # need to add that map or possibly expost whatever we have in jnr-posix
        raise Errno::EACCES.new('File.symlink') if errno == 1314 # ERROR_PRIVILEGE_NOT_HELD
        raise SystemCallError.new('File.symlink', errno)
      end

      0 # Comply with spec
    end

    private

    # Simulate Ruby's string checking
    def self.string_check(arg)
      return arg if arg.is_a?(String)
      return arg.send(:to_str) if arg.respond_to?(:to_str, true) # MRI allows private to_str
      return arg.to_path if arg.respond_to?(:to_path)
      raise TypeError
    end
  end

rescue LoadError, FFI::NotFoundError

  # Could not load FFI or CreateSymbolicLinkW unavailable, define symlink functions to raise NotImplemented
  class ::File
    def self.symlink(*)
      raise NotImplementedError.new("symlink not supported on this version of Windows")
    end
  end

end
