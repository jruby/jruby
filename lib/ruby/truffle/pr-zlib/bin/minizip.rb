# minigzip.rb -- simulate gzip using the zlib compression library
# Copyright (C) 1995-2005 Jean-loup Gailly.
# For conditions of distribution and use, see copyright notice in rbzlib.rb
#
#
#
# minigzip is a minimal implementation of the gzip utility. This is
# only an example of using zlib and isn't meant to replace the
# full-featured gzip. No attempt is made to deal with file systems
# limiting names to 14 or 8+3 characters, etc... Error checking is
# very limited. So use minigzip only for testing; use gzip for the
# real thing. On MSDOS, use only on file names without extension
# or in pipe mode.
#              
# Ruby tralnslation By Park Heesob.

require 'rbzlib'
include Rbzlib

GZ_SUFFIX = ".gz"
SUFFIX_LEN = GZ_SUFFIX.length

BUFLEN      = 16384
MAX_NAME_LEN = 1024


def error(msg)
    puts("#{__FILE__}: #{msg}")
    exit(1)
end

def gz_compress(_in, out)
    while(true)
      begin
        buf = _in.read(BUFLEN)
      rescue 
        raise RuntimeError,"read"
      end
      break if buf.nil?
      err = 0
      len = buf.length
      if (gzwrite(out, buf, len) != len)
        error(gzerror(out, err))
      end
    end
    _in.close
    error("failed gzclose") if (gzclose(out) != Z_OK) 
end


def gz_uncompress(_in, out)
    buf = 0.chr * BUFLEN
    while true
        len = gzread(_in, buf, buf.length)
        err = 0
        error(gzerror(_in, err)) if (len < 0) 
        break if len.zero?
		if(out.write(buf[0,len]) != len)
            error("failed write")
        end
    end
    begin
      out.close
    rescue
      error("failed fclose") 
    end

    error("failed gzclose") if (gzclose(_in) != Z_OK) 
end


def file_compress(file, mode)
    outfile = file + GZ_SUFFIX

    _in = File.open(file, "rb")
    if _in.nil? 
        raise RuntimeError,file
    end
    out = gzopen(outfile, mode)
    if out.nil?
        puts("#{__FILE__}: can't gzopen #{outfile}")
        exit(1)
    end
    gz_compress(_in, out)

    File.unlink(file)
end


def file_uncompress(file)
    len = file.length
    buf = file.dup

    if (file[-SUFFIX_LEN..-1] == GZ_SUFFIX)
        infile = file.dup
        outfile = buf[0..(-SUFFIX_LEN-1)]
    else 
        outfile = file.dup
        infile = buf + GZ_SUFFIX
    end
    _in = gzopen(infile, "rb")
    if _in.nil? 
        puts("#{__FILE__}: can't gzopen #{infile}")
        exit(1)
    end
    out = File.open(outfile, "wb")
    if out.nil? 
        raise RuntimeError,file
        exit(1)
    end

    gz_uncompress(_in, out)

    File.unlink(infile)
end


#===========================================================================
# Usage:  minigzip [-d] [-f] [-h] [-r] [-1 to -9] [files...]
#  -d : decompress
#  -f : compress with Z_FILTERED
#  -h : compress with Z_HUFFMAN_ONLY
#  -r : compress with Z_RLE
#  -1 to -9 : compression level
#

    uncompr = false

    outmode = "wb6 "

    while !ARGV.empty?
      argv = ARGV.shift
      case argv
      when "-d"
        uncompr = true
      when "-f"
        outmode[3] = 'f'
      when "-h"
        outmode[3] = 'h'
      when "-r"
        outmode[3] = 'R'
      when "-1".."-9"
        outmode[2] = argv[1]
      else
        ARGV.unshift(argv)
        break
      end
    end
    if (outmode[3].chr == ' ')
        outmode = outmode[0,3]
    end
    if (ARGV.empty?)        
        $stdin.binmode
        $stdout.binmode
        if (uncompr) 
            file = gzdopen($stdin.fileno, "rb")
            error("can't gzdopen stdin") if file.nil? 
            gz_uncompress(file, $stdout)
        else 
            file = gzdopen($stdout.fileno, outmode)
            error("can't gzdopen stdout") if file.nil? 
            gz_compress($stdin, file)
        end
    else 
        while !ARGV.empty? 
            if (uncompr) 
                file_uncompress(ARGV.shift)
            else 
                file_compress(ARGV.shift, outmode)
            end
        end
    end

