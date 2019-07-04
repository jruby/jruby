# JRuby 1.7.x and lower implements most of tempfile as a JRuby ext. This
# file simple loads that ext and then adds the additional functions that
# control how tempfile name is generated.

require 'delegate' # Unused; here for compatibility
require 'tmpdir'
require 'thread' # Unused; here for compatibility

# Load built-in tempfile library
JRuby::Util.load_ext("org.jruby.ext.tempfile.TempfileLibrary")

class Tempfile
  include Dir::Tmpname

  class << self
    # Creates a new Tempfile.
    #
    # If no block is given, this is a synonym for Tempfile.new.
    #
    # If a block is given, then a Tempfile object will be constructed,
    # and the block is run with said object as argument. The Tempfile
    # object will be automatically closed after the block terminates.
    # The call returns the value of the block.
    #
    # In any case, all arguments (<code>*args</code>) will be passed to Tempfile.new.
    #
    #   Tempfile.open('foo', '/home/temp') do |f|
    #      # ... do something with f ...
    #   end
    #
    #   # Equivalent:
    #   f = Tempfile.open('foo', '/home/temp')
    #   begin
    #      # ... do something with f ...
    #   ensure
    #      f.close
    #   end
    def open(*args)
      tempfile = new(*args)

      if block_given?
        begin
          yield(tempfile)
        ensure
          tempfile.close
        end
      else
        tempfile
      end
    end
  end
end

# Creates a temporally file as usual File object (not Tempfile).
# It don't use finalizer and delegation.
#
# If no block is given, this is similar to Tempfile.new except
# creating File instead of Tempfile.
# The created file is not removed automatically.
# You should use File.unlink to remove it.
#
# If a block is given, then a File object will be constructed,
# and the block is invoked with the object as the argument.
# The File object will be automatically closed and
# the temporally file is removed after the block terminates.
# The call returns the value of the block.
#
# In any case, all arguments (+*args+) will be treated as Tempfile.new.
#
#   Tempfile.create('foo', '/home/temp') do |f|
#      ... do something with f ...
#   end
#
def Tempfile.create(basename="", tmpdir=nil, mode: 0, **options)
  tmpfile = nil
  Dir::Tmpname.create(basename, tmpdir, options) do |tmpname, n, opts|
    mode |= File::RDWR|File::CREAT|File::EXCL
    opts[:perm] = 0600
    tmpfile = File.open(tmpname, mode, opts)
  end
  if block_given?
    begin
      yield tmpfile
    ensure
      unless tmpfile.closed?
        if File.identical?(tmpfile, tmpfile.path)
          unlinked = File.unlink tmpfile.path rescue nil
        end
        tmpfile.close
      end
      unless unlinked
        begin
          File.unlink tmpfile.path
        rescue Errno::ENOENT
        end
      end
    end
  else
    tmpfile
  end
end
