require "tempfile"
require "rbconfig.rb"
require "rubicon_tests"

#
# Simple wrapper for RubyUnit, primarily designed to capture
# statistics and report them at the end.
#

# -------------------------------------------------------------
#
# Operating system classification. We use classes for this, as 
# we get lots of flexibility with comparisons.
#
# Use with
#
#   Unix.or_variant do ... end        # operating system is some Unix variant
#
#   Linux.only do .... end            # operating system is Linux
#
#   MsWin32.dont do .... end          # don't run under MsWin32
#
# If there is a known problem which is very, very unlikely ever to be
# fixed, you can say
#
#   Cygwin.known_problem do
#
#   end
#
# This runs the test, but squelches the error on that particular operating
# system

class OS
  def OS.or_variant
    yield if $os <= self
  end

  def OS.only
    yield if $os == self
  end

  def OS.dont
    yield unless $os <= self
  end

  def OS.known_problem
    if $os <= self
      begin
        yield
      rescue Test::Unit::AssertionFailedError => err
        $stderr.puts
        $stderr.puts
        $stderr.puts "Ignoring known problem: #{err.message}"
        $stderr.puts caller[0]
        $stderr.puts
      end
    else
      yield
    end
  end
end

class JRuby   < OS;      end
class Unix    < OS;      end
class Windows < OS;      end

class BSD     < Unix;    end
class HPUX    < Unix;    end
class Linux   < Unix;    end
class MacOS   < Unix;    end
class Solaris < Unix;    end

class FreeBSD < BSD;     end

class Cygwin  < Windows; end
class WindowsNative < Windows; end

class MinGW   < WindowsNative; end
class MsWin32 < WindowsNative; end

$os = case RUBY_PLATFORM
      when /linux/   then  Linux
      when /freebsd/ then FreeBSD
      when /bsd/     then BSD
      when /solaris/ then Solaris
      when /hpux/    then HPUX
      when /cygwin/  then Cygwin
      when /mswin32/ then MsWin32
      when /mingw32/ then MinGW
      when /java/    then JRuby
      when /powerpc-darwin/    then MacOS
      else OS
      end

#
# Find the name of the interpreter.
# 

$interpreter = File.join(Config::CONFIG["bindir"], 
			 Config::CONFIG["RUBY_INSTALL_NAME"])

MsWin32.or_variant { $interpreter.tr! '/', '\\' }

#
# Classification routines. We use these so that the code can
# test for operating systems, ruby versions, and other features
# without being platform specific
#

# -------------------------------------------------------
# Class to manipulate Ruby version numbers. We use this to 
# insulate ourselves from changes in version number format.
# Independent of the internal representation, we always allow 
# comparison against a string.
#
# Use in the code with stuff like:
#
#    Version.greater_than("1.6.2") do
#       assert(...)
#    end
#
# or like
#
#    if Version <= "1.8.0"
#      assert(...)
#    end
#

class LanguageVersion
  include Comparable

  def initialize
    # RUBY_VERSION is introduced with 1.9.x
    # VERSION was used until 1.9.x
    @version = defined?(RUBY_VERSION) ? RUBY_VERSION : VERSION
  end
  
  # This method defines how to compare versions, and should be the only
  # place where such a comparison is made. Other methods, like
  # LanguageVersion.greater_than, use this method at a lower level.
  #
  # The argument is a string (not another LanguageVersion object).
  #
  def <=>(other)
    @version <=> other
  end

  # Specify a range of versions, and run a test block if the current version
  # falls within that range.  
  #
  def in(range)
    # TODO: eliminate dependency on Range#include?. If the version
    # comparison becomes more complicated than a simple lexicographical
    # comparison, we need to take care of this ourselves. Then we
    # should use LanguageVersion#<=> here too, so we have *one* point
    # where this is defined.
    #
    if range.include?(@version)
      yield
    end
  end

  def greater_than(version)
    if self > version
      yield
    end
  end

  def greater_or_equal(version)
    if self >= version
      yield
    end
  end

  def less_than(version)
    if self < version
      yield
    end
  end

  def less_or_equal(version)
    if self <= version
      yield
    end
  end
 
end

Version = LanguageVersion.new

