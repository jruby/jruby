# Windows symlink support borrowed from djberg96/win32-file and ffi-win32-extensions

if org.jruby.platform.Platform::IS_WINDOWS

  begin
    require 'ffi'
  rescue LoadError
    # Gracefully bail if FFI is not available
  end

  if defined?(::FFI)
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

          begin
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

            # We use the presence or absence of this method to indicate everything bound successfully (jruby/jruby#3998)
            attach_pfunc :CreateSymbolicLinkW, [:buffer_in, :buffer_in, :dword], :bool
          rescue FFI::NotFoundError
            # We are unable to implement symbolic links on this version of Windows
          end
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
                :cFileName, [:uint8, 260*2],
                :cAlternateFileName, [:uint8, 14*2]
            )
          end
        end
      end
    end

    # Since we only do this for symlink, skip it all if it's not available on this version of Windows (jruby/jruby#3998)
    if JRuby::Windows::File::Functions.respond_to? :CreateSymbolicLinkW
      class File
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
            raise SystemCallError.new('CreateSymbolicLink', FFI.errno)
          end

          0 # Comply with spec
        end

        # Returns whether or not +file+ is a symlink.
        #
        def self.symlink?(file)
          return false unless File.exist?(file)

          bool  = false
          wfile = string_check(file).wincode

          attrib = GetFileAttributesW(wfile)

          if attrib == INVALID_FILE_ATTRIBUTES
            raise SystemCallError.new('GetFileAttributes', FFI.errno)
          end

          if attrib & FILE_ATTRIBUTE_REPARSE_POINT > 0
            begin
              find_data = WIN32_FIND_DATA.new
              handle = FindFirstFileW(wfile, find_data)

              if handle == INVALID_HANDLE_VALUE
                raise SystemCallError.new('FindFirstFile', FFI.errno)
              end

              if find_data[:dwReserved0] == IO_REPARSE_TAG_SYMLINK
                bool = true
              end
            ensure
              CloseHandle(handle)
            end
          end

          bool
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

      class String
        # Convenience method for converting strings to UTF-16LE for wide character
        # functions that require it.
        #
        def wincode
          (self.tr(File::SEPARATOR, File::ALT_SEPARATOR) + 0.chr).encode('UTF-16LE')
        end
      end
    end
  end
end
