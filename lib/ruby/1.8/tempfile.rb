#
# tempfile - manipulates temporary files
#
# $Id$
#

require 'thread'
require 'delegate'
require 'tmpdir'

# A class for managing temporary files.  This library is written to be
# thread safe.
class Tempfile < DelegateClass(File)
  MAX_TRY = 10

  class FinalizerData < Struct.new :tmpname, :tmpfile, :mutex # :nodoc:
  end

  # Creates a temporary file of mode 0600 in the temporary directory
  # whose name is basename.pid.n and opens with mode "w+".  A Tempfile
  # object works just like a File object.
  #
  # If tmpdir is omitted, the temporary directory is determined by
  # Dir::tmpdir provided by 'tmpdir.rb'.
  # When $SAFE > 0 and the given tmpdir is tainted, it uses
  # /tmp. (Note that ENV values are tainted by default)
  def initialize(basename, tmpdir=Dir::tmpdir)
    if $SAFE > 0 and tmpdir.tainted?
      tmpdir = '/tmp'
    end

    failure = 0
    begin
      @tmpname = File.join(tmpdir, make_tmpname_secure(basename))
      @tmpfile = File.open(@tmpname, File::RDWR|File::CREAT|File::EXCL, 0600)
    rescue Exception => e
      failure += 1
      retry if failure < MAX_TRY
      raise "cannot generate tempfile `%s': #{e.message}" % @tmpname
    end

    @mutex = Mutex.new

    @data = FinalizerData[@tmpname, @tmpfile, @mutex]
    @clean_proc = Tempfile.callback(@data)
    ObjectSpace.define_finalizer(self, @clean_proc)

    super(@tmpfile)

    # Now we have all the File/IO methods defined, you must not
    # carelessly put bare puts(), etc. after this.
  end

  @@sequence_number = 0
  @@sequence_mutex = Mutex.new
  def make_tmpname_secure(basename) #:nodoc:
    begin
      File.open("/dev/urandom", "rb") do |random|
        basename = "#{random.read(16).unpack('H*')}_#{basename}"
      end
    rescue
    end
    sequence_number = @@sequence_mutex.synchronize { @@sequence_number += 1 }
    make_tmpname(basename, sequence_number)
  end
  private :make_tmpname_secure

  def make_tmpname(basename, n)
    "#{basename}.#{$$}.#{n}"
  end
  private :make_tmpname

  # Opens or reopens the file with mode "r+".
  def open
    @mutex.synchronize do
      @tmpfile.close if @tmpfile
      @tmpfile = File.open(@tmpname, 'r+')
      @data.tmpfile = @tmpfile
      __setobj__(@tmpfile)
    end
  end

  def _close	# :nodoc:
    @tmpfile.close if @tmpfile
    @data.tmpfile = @tmpfile = nil
  end    
  protected :_close

  # Closes the file.  If the optional flag is true, unlinks the file
  # after closing.
  #
  # If you don't explicitly unlink the temporary file, the removal
  # will be delayed until the object is finalized.
  def close(unlink_now=false)
    if unlink_now
      close!
    else
      @mutex.synchronize { _close }
    end
  end

  # Closes and unlinks the file.
  def close!
    @mutex.synchronize do
      _close
      @data.mutex = nil # @clean_proc does not need to acquire the lock here
      @clean_proc.call
      ObjectSpace.undefine_finalizer(self)
    end
  end

  # Unlinks the file.  On UNIX-like systems, it is often a good idea
  # to unlink a temporary file immediately after creating and opening
  # it, because it leaves other programs zero chance to access the
  # file.
  def unlink
    @mutex.synchronize do
      begin
        begin
          File.unlink(@tmpname)
        rescue Errno::ENOENT
        end
        @data = @tmpname = nil
        ObjectSpace.undefine_finalizer(self)
      rescue Errno::EACCES
        # may not be able to unlink on Windows; just ignore
      end
    end
  end
  alias delete unlink

  # Returns the full path name of the temporary file.
  def path
    @mutex.synchronize { @tmpname.dup }
  end

  # Returns the size of the temporary file.  As a side effect, the IO
  # buffer is flushed before determining the size.
  def size
    @mutex.synchronize do
      if @tmpfile
        @tmpfile.flush
        @tmpfile.stat.size
      else
        0
      end
    end
  end
  alias length size

  class << self
    def callback(data)	# :nodoc:
      pid = $$
      lambda{
	if pid == $$ 
          data.mutex.lock if data.mutex
          begin
	    print "removing ", data.tmpname, "..." if $DEBUG

	    data.tmpfile.close if data.tmpfile

	    # keep this order for thread safeness
	    begin
	      File.unlink(data.tmpname)
            rescue Errno::ENOENT, Errno::ENOTDIR, Errno::EISDIR
            end

	    print "done\n" if $DEBUG
          ensure
            data.mutex.unlock if data.mutex
          end
	end
      }
    end

    # If no block is given, this is a synonym for new().
    #
    # If a block is given, it will be passed tempfile as an argument,
    # and the tempfile will automatically be closed when the block
    # terminates.  In this case, open() returns nil.
    def open(*args)
      tempfile = new(*args)

      if block_given?
	begin
	  yield(tempfile)
	ensure
	  tempfile.close
	end

	nil
      else
	tempfile
      end
    end
  end
end

if __FILE__ == $0
#  $DEBUG = true
  f = Tempfile.new("foo")
  f.print("foo\n")
  f.close
  f.open
  p f.gets # => "foo\n"
  f.close!
end
