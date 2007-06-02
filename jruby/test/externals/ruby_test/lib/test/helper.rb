##############################################################################
# This module contains helper methods for use within the test cases included
# in this package.  Most of them are cross platform helpers for figuring out
# user names, home directories, etc.
##############################################################################
require 'rubygems' rescue nil
require 'ptools'
require 'pathname2'

unless defined? JRUBY_VERSION
   require 'sys/admin'
   include Sys
end

# Use this for assert_kind_of tests where the return value is true or false.
module Boolean; end
class TrueClass; include Boolean; end
class FalseClass; include Boolean; end

module Test
   module Helper
      if defined? JRUBY_VERSION
         JRUBY = true
      else
         JRUBY = false
      end

      if RUBY_PLATFORM.match('mswin')
         MAX_PATH = 260
         WINDOWS = true
         require 'Win32API'

         GetUserName = Win32API.new('advapi32', 'GetUserName', 'PL', 'I')
         GetShortPathName = Win32API.new('kernel32',
            'GetShortPathName', 'PPL', 'L'
         )
      else
         WINDOWS = false
         require 'etc' unless JRUBY
      end

      if RUBY_PLATFORM.match('linux')
         LINUX = true
      else
         LINUX = false
      end

      if RUBY_PLATFORM.match('solaris')
         SOLARIS = true
      else
         SOLARIS = false
      end

      if RUBY_PLATFORM.match('bsd')
         BSD = true
      else
         BSD = false
      end
      
      if RUBY_PLATFORM.match('vms')
         VMS = true
      else
         VMS = false
      end

      if RUBY_PLATFORM =~ /darwin|mach|osx/i
         OSX = true
      else
         OSX = false
      end

      RELEASE = RUBY_VERSION.split('.').last.to_i

      # True if tests are run on a big endian platform
      BIG_ENDIAN = [1].pack('I') == [1].pack('N')

      # True if tests are run in 64-bit mode
      BIT_64 = (2**33).is_a?(Fixnum)

      # True if tests are run in 32-bit mode
      BIT_32 = (2**33).is_a?(Bignum)

      # True if the current process is running as root
      if JRUBY
         ROOT = false
      else
         ROOT = Process.euid == 0
      end

      # Returns the base directory of the current file.
      #
      def base_dir(file)
         File.expand_path(File.dirname(file))
      end

      # Returns the base of +path+ with +file+ appended to the end.
      #
      def base_file(path, file)
         File.expand_path(File.dirname(path)) + '/' + file
      end

      # Create an simple file. If +text+ is provided, it's written to the
      # file. Otherwise, an empty file is created.
      #
      def touch(file, text=nil)
         if text
            File.open(file, 'w'){ |fh| fh.puts text }
         else
            File.open(file, 'w'){}
         end
      end

      # This uses a native (system) touch command. Used for various File tests
      # where we don't want to use Ruby's own File methods.
      #
      def touch_n(file)
         system("touch #{file}")
      end

      # Get the user of the current process.
      #
      def get_user
         user = ENV['USERNAME'] || ENV['USER']
         if WINDOWS
            if user.nil?
               buf = 0.chr * MAX_PATH
               if GetUserName.call(buf, buf.length) == 0
                  raise "Unable to get user name"
               end
               user = buf.unpack("A*")
            end
         else
            unless JRUBY
               user ||= Etc.getpwuid(Process.uid).name
            end
         end
         user
      end

      # Returns the home directory of the current process owner.
      # 
      def get_home
         home = ENV['HOME'] || ENV['USERPROFILE']
         if WINDOWS
            home ||= "C:\\Documents and Settings\\" + get_user
         else
            unless JRUBY
               home ||= Etc.getpwuid(Process.uid).dir
            end
         end
         home
      end
      
      # Removes +file+ in a platform independent manner using system calls.
      # Also handles paths with spaces in them.
      # 
      def remove_file(file)
         if WINDOWS
            buf = 0.chr * 260
            GetShortPathName.call(file, buf, buf.size)
            file = buf.unpack("A*").first
            file.tr!('/', '\\')
            system("del /f /q #{file}") if File.exists?(file)
         else
            system("rm -f #{file}") if File.exists?(file)
         end
      end

      # Removes +dir+ in a platform independent manner using system calls.
      def remove_dir(dir)
         if WINDOWS
            system("rmdir /s /q #{dir}")
         else
            system("rm -rf #{dir}")
         end
      end
   end
end

# This comes courtesy of Edwin Fine.
module Test
  module Unit
    module Assertions

      private

      def _expected_exception_or_subclass?(actual_exception, exceptions, modules) # :nodoc:
        exceptions.any? {|cls| actual_exception.class <= cls } or
        modules.any? {|mod| actual_exception.is_a?(mod)}
      end

      ##
      # Passes if the block raises one of the given exceptions or its descendants.
      #
      # Example:
      #   assert_raise_s RuntimeError, LoadError do
      #     raise 'Boom!!!'
      #   end

      public

      def assert_raise_kind_of(*args)
        _wrap_assertion do
          if Module === args.last
            message = ""
          else
            message = args.pop
          end
          exceptions, modules = _check_exception_class(args)
          expected = args.size == 1 ? args.first : args
          actual_exception = nil
          full_message = build_message(message, "<?> exception expected but none was thrown.", expected)
          assert_block(full_message) do
            begin
              yield
            rescue Exception => actual_exception
              break
            end
            false
          end
          full_message = build_message(message, "<?> exception expected but was\n?", expected, actual_exception)
          assert_block(full_message) {_expected_exception_or_subclass?(actual_exception, exceptions, modules)}
          actual_exception
        end
      end
    end
  end
end
