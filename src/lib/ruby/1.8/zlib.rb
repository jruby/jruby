module Zlib

    # Constants
    ZLIB_VERSION = "1.2.1"
    BINARY = 0
    MAX_MEM_LEVEL = 9
    OS_UNIX = 3
    OS_UNKNOWN = 255
    HUFFMAN_ONLY = 2
    OS_CODE = 11
    OS_ZSYSTEM = 8
    BEST_SPEED = 1
    FULL_FLUSH = 3
    OS_MACOS = 7
    DEF_MEM_LEVEL = 8
    OS_VMS = 2
    OS_RISCOS = 13
    FILTERED = 1
    OS_VMCMS = 4
    NO_COMPRESSION = 0
    SYNC_FLUSH = 2
    OS_OS2 = 6
    VERSION = "0.6.0"
    MAX_WBITS = 15
    OS_AMIGA = 1
    OS_QDOS = 12
    UNKNOWN = 2
    DEFAULT_COMPRESSION = -1
    OS_WIN32 = 11
    NO_FLUSH = 0
    OS_ATARI = 5
    DEFAULT_STRATEGY = 0
    OS_MSDOS = 0
    OS_CPM = 9
    ASCII = 1
    BEST_COMPRESSION = 9
    FINISH = 4
    OS_TOPS20 = 10

    # module Methods

    def Zlib.zlib_version
    end

    def Zlib.version
    end

    def Zlib.adler32
    end

    def Zlib.crc32
    end

    def Zlib.crc_table
    end

    # Methods
end

class Zlib::Error < StandardError

    # Constants

    # class Methods

    # Methods
end

class Zlib::StreamEnd < Zlib::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::StreamError < Zlib::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::ZStream < Object

    # Constants

    # class Methods

    # Methods

    def flush_next_out
    end

    def total_out
    end

    def stream_end?
    end

    def data_type
    end

    def closed?
    end

    def ended?
    end

    def end
    end

    def reset
    end

    def avail_out
    end

    def avail_out=
    end

    def adler
    end

    def finish
    end

    def avail_in
    end

    def flush_next_in
    end

    def total_in
    end

    def finished?
    end

    def close
    end
end

class Zlib::Inflate < Zlib::ZStream

    # Constants

    # class Methods

    def self.inflate
    end

    # Methods

    def <<
    end

    def sync_point?
    end

    def set_dictionary
    end

    def inflate
    end

    def sync
    end
end

class Zlib::GzipFile

    # Constants

    # class Methods

    def self.wrap
    end

    # Methods

    def os_code
    end

    def closed?
    end

    def orig_name
    end

    def to_io
    end

    def finish
    end

    def comment
    end

    def crc
    end

    def mtime
    end

    def sync
    end

    def close
    end

    def level
    end

    def sync=
    end
end

class Zlib::GzipReader < Zlib::GzipFile

    # Constants

    # class Methods

    def self.open
    end

    # Methods

    def reject
    end

    def rewind
    end

    def member?
    end

    def lineno
    end

    def readline
    end

    def find
    end

    def each_with_index
    end

    def read
    end

    def lineno=
    end

    def collect
    end

    def all?
    end

    def entries
    end

    def pos
    end

    def readchar
    end

    def readlines
    end

    def detect
    end

    def zip
    end

    def each_byte
    end

    def getc
    end

    def map
    end

    def any?
    end

    def sort
    end

    def eof
    end

    def min
    end

    def ungetc
    end

    def find_all
    end

    def each
    end

    def inject
    end

    def sort_by
    end

    def max
    end

    def select
    end

    def unused
    end

    def eof?
    end

    def partition
    end

    def gets
    end

    def grep
    end

    def include?
    end

    def tell
    end
end

class Zlib::GzipFile::Error < Zlib::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::GzipFile::CRCError < Zlib::GzipFile::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::GzipFile::NoFooter < Zlib::GzipFile::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::GzipFile::LengthError < Zlib::GzipFile::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::VersionError < Zlib::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::Deflate < Zlib::ZStream

    # Constants

    # class Methods

    def self.deflate
    end

    # Methods

    def <<
    end

    def params
    end

    def set_dictionary
    end

    def flush
    end

    def deflate
    end
end

class Zlib::DataError < Zlib::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::GzipWriter < Zlib::GzipFile

    # Constants

    # class Methods

    def self.open
    end

    # Methods

    def <<
    end

    def printf
    end

    def print
    end

    def pos
    end

    def orig_name=
    end

    def putc
    end

    def comment=
    end

    def puts
    end

    def flush
    end

    def mtime=
    end

    def tell
    end

    def write
    end
end

class Zlib::GzipFile::CRCError < Zlib::GzipFile::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::GzipFile::Error < Zlib::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::GzipFile::NoFooter < Zlib::GzipFile::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::GzipFile::LengthError < Zlib::GzipFile::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::BufError < Zlib::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::GzipFile::CRCError < Zlib::GzipFile::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::GzipFile::Error < Zlib::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::GzipFile::NoFooter < Zlib::GzipFile::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::GzipFile::LengthError < Zlib::GzipFile::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::NeedDict < Zlib::Error

    # Constants

    # class Methods

    # Methods
end

class Zlib::MemError < Zlib::Error

    # Constants

    # class Methods

    # Methods
end