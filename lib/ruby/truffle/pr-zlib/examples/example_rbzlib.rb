# example.rb -- usage example of the zlib compression library
# Copyright (C) 1995-2004 Jean-loup Gailly.
# For conditions of distribution and use, see copyright notice in rbzlib.rb
#
# Ruby translation by Park Heesob

require 'rbzlib'
include Rbzlib

def CHECK_ERR(err,msg)
  if(err != Z_OK)
    raise RuntimeError,"#{msg} error: #{err}"
  end
end

TESTFILE = "foo.gz"
@@hello = "hello, hello!\0"

@@dictionary = "hello"

def test_compress(compr, comprLen, uncompr, uncomprLen)
    len = @@hello.length

    err,comprLen = compress(compr, comprLen, @@hello, len)
    CHECK_ERR(err, "compress")
    compr = compr[0,comprLen]
    uncompr[0,7] = "garbage"
    err,uncomprLen = uncompress(uncompr, uncomprLen, compr, comprLen)
    CHECK_ERR(err, "uncompress")
    uncompr = uncompr[0,uncomprLen]
    if uncompr != @@hello
        puts("bad uncompress")
        exit(1)
    else
        puts("uncompress(): #{uncompr}")
    end
end

def test_gzio(fname, uncompr, uncomprLen)
    len = @@hello.length
    err = 0
    file = gzopen(fname, "wb")
    if file.nil?
        puts("gzopen error")
        exit(1)
    end
    gzputc(file, 'h')
    if (gzputs(file, "ello") != 4)
        puts("gzputs err: #{gzerror(file, err)}")
        exit(1)
    end
    if (gzputs(file, ", hello!") != 8)
        puts("gzputs err: #{gzerror(file, err)}")
        exit(1)
    end
    gzseek(file, 1, SEEK_CUR)
    gzclose(file)

    file = gzopen(fname, "rb")
    if file.nil?
        puts("gzopen error")
        exit(1)
    end
    uncompr[0,7] = "garbage"
    if (gzread(file, uncompr, uncomprLen) != len)
        puts("gzread err: #{gzerror(file, err)}")
        exit(1)
    end
    uncompr = uncompr[0,len]
    if uncompr != @@hello
        puts("bad gzread: #{uncompr}")
        exit(1)
    else
        puts("gzread(): #{uncompr}")
    end
    pos = gzseek(file, -8, SEEK_CUR)
    if (pos != 6 || gztell(file) != pos)
        puts("gzseek error, pos=#{pos}, gztell=#{gztell(file)}")
        exit(1)
    end

    if (gzgetc(file) != ' ')
        puts("gzgetc error")
        exit(1)
    end

    if (gzungetc(' ', file) != ' ')
        puts("gzungetc error")
        exit(1)
    end

    gzgets(file, uncompr, uncomprLen)
    uncompr.chop!
    if uncompr.length != 7
        puts("gzgets err after gzseek: #{gzerror(file, err)}")
        exit(1)
    end

    if uncompr != @@hello[6..-2]
        puts("bad gzgets after gzseek")
        exit(1)
    else
        puts("gzgets() after gzseek: #{uncompr}")
    end

    gzclose(file)
end

def test_deflate(compr, comprLen)
    c_stream = Z_stream.new
    len = @@hello.length

    err = deflateInit(c_stream, Z_DEFAULT_COMPRESSION)
    CHECK_ERR(err, "deflateInit")

    c_stream.next_in  = Bytef.new(@@hello)
    c_stream.next_out = Bytef.new(compr)

    while (c_stream.total_in != len && c_stream.total_out < comprLen)
        c_stream.avail_in = c_stream.avail_out = 1
        err = deflate(c_stream, Z_NO_FLUSH)
        CHECK_ERR(err, "deflate")
    end
    while true
        c_stream.avail_out = 1
        err = deflate(c_stream, Z_FINISH)
        break if (err == Z_STREAM_END)
        CHECK_ERR(err, "deflate")
    end

    err = deflateEnd(c_stream)
    CHECK_ERR(err, "deflateEnd")
end

def test_inflate(compr, comprLen, uncompr, uncomprLen)
    uncompr[0,7] = "garbage"
    d_stream = Z_stream.new

    d_stream.next_in  = Bytef.new(compr)
    d_stream.avail_in = 0
    d_stream.next_out = Bytef.new(uncompr)

    err = inflateInit(d_stream)
    CHECK_ERR(err, "inflateInit")

    while (d_stream.total_out < uncomprLen && d_stream.total_in < comprLen)
        d_stream.avail_in = d_stream.avail_out = 1
        err = inflate(d_stream, Z_NO_FLUSH)
        break if (err == Z_STREAM_END)
        CHECK_ERR(err, "inflate")
    end

    err = inflateEnd(d_stream)
    CHECK_ERR(err, "inflateEnd")
    uncompr = uncompr[0,d_stream.total_out]
    if uncompr != @@hello
        puts("bad inflate")
        exit(1)
    else
        puts("inflate(): #{uncompr}")
    end
end

def test_large_deflate(compr, comprLen, uncompr, uncomprLen)
    c_stream = Z_stream.new
    err = deflateInit(c_stream, Z_BEST_SPEED)
    CHECK_ERR(err, "deflateInit")

    c_stream.next_out = Bytef.new(compr)
    c_stream.avail_out = comprLen
    c_stream.next_in = Bytef.new(uncompr)
    c_stream.avail_in = uncomprLen
    err = deflate(c_stream, Z_NO_FLUSH)
    CHECK_ERR(err, "deflate")
    if c_stream.avail_in.nonzero?
        puts("deflate not greedy")
        exit(1)
    end

    deflateParams(c_stream, Z_NO_COMPRESSION, Z_DEFAULT_STRATEGY)
    c_stream.next_in = Bytef.new(compr)
    c_stream.avail_in = comprLen/2
    err = deflate(c_stream, Z_NO_FLUSH)
    CHECK_ERR(err, "deflate")

    deflateParams(c_stream, Z_BEST_COMPRESSION, Z_FILTERED)
    c_stream.next_in = Bytef.new(uncompr)
    c_stream.avail_in = uncomprLen
    err = deflate(c_stream, Z_NO_FLUSH)
    CHECK_ERR(err, "deflate")

    err = deflate(c_stream, Z_FINISH)
    if (err != Z_STREAM_END)
        puts("deflate should report Z_STREAM_END")
        exit(1)
    end
    err = deflateEnd(c_stream)
    CHECK_ERR(err, "deflateEnd")
end

def test_large_inflate(compr, comprLen, uncompr, uncomprLen)
    d_stream = Z_stream.new
    uncompr[0,7] = "garbage"

    d_stream.next_in  = Bytef.new(compr)
    d_stream.avail_in = comprLen

    err = inflateInit(d_stream)
    CHECK_ERR(err, "inflateInit")

    while true
        d_stream.next_out = Bytef.new(uncompr)
        d_stream.avail_out = uncomprLen
        err = inflate(d_stream, Z_NO_FLUSH)
        break if (err == Z_STREAM_END)
        CHECK_ERR(err, "large inflate")
    end

    err = inflateEnd(d_stream)
    CHECK_ERR(err, "inflateEnd")

    if (d_stream.total_out != 2*uncomprLen + comprLen/2)
        puts("bad large inflate: #{d_stream.total_out}")
        exit(1)
    else
        puts("large_inflate(): OK")
    end
end

def test_flush(compr, comprLen)
    c_stream = Z_stream.new
    len = @@hello.length

    err = deflateInit(c_stream, Z_DEFAULT_COMPRESSION)
    CHECK_ERR(err, "deflateInit")

    c_stream.next_in  = Bytef.new(@@hello)
    c_stream.next_out = Bytef.new(compr)
    c_stream.avail_in = 3
    c_stream.avail_out = comprLen
    err = deflate(c_stream, Z_FULL_FLUSH)
    CHECK_ERR(err, "deflate")

    compr[3]=(compr[3].ord+1).chr
    c_stream.avail_in = len - 3

    err = deflate(c_stream, Z_FINISH)
    if (err != Z_STREAM_END)
        CHECK_ERR(err, "deflate")
    end
    err = deflateEnd(c_stream)
    CHECK_ERR(err, "deflateEnd")

    comprLen = c_stream.total_out
end

def test_sync(compr, comprLen, uncompr, uncomprLen)
    d_stream = Z_stream.new
    uncompr[0,7] = "garbage"

    d_stream.next_in  = Bytef.new(compr)
    d_stream.avail_in = 2

    err = inflateInit(d_stream)
    CHECK_ERR(err, "inflateInit")

    d_stream.next_out = Bytef.new(uncompr)
    d_stream.avail_out = uncomprLen

    inflate(d_stream, Z_NO_FLUSH)
    CHECK_ERR(err, "inflate")

    d_stream.avail_in = comprLen-2
    err = inflateSync(d_stream)
    CHECK_ERR(err, "inflateSync")

    err = inflate(d_stream, Z_FINISH)
    if (err != Z_DATA_ERROR)
        puts("inflate should report DATA_ERROR")
        exit(1)
    end
    err = inflateEnd(d_stream)
    uncompr = uncompr[0,d_stream.total_out]
    CHECK_ERR(err, "inflateEnd")
    puts("after inflateSync(): hel#{uncompr}")
end

def test_dict_deflate(compr, comprLen)
    c_stream = Z_stream.new
    err = deflateInit(c_stream, Z_BEST_COMPRESSION)
    CHECK_ERR(err, "deflateInit")

    err = deflateSetDictionary(c_stream,@@dictionary, @@dictionary.length)
    CHECK_ERR(err, "deflateSetDictionary")

    @@dictId = c_stream.adler
    c_stream.next_out = Bytef.new(compr)
    c_stream.avail_out = comprLen

    c_stream.next_in = Bytef.new(@@hello)
    c_stream.avail_in = @@hello.length

    err = deflate(c_stream, Z_FINISH)
    if (err != Z_STREAM_END)
        puts("deflate should report Z_STREAM_END")
        exit(1)
    end
    err = deflateEnd(c_stream)
    CHECK_ERR(err, "deflateEnd")
end

def test_dict_inflate(compr, comprLen, uncompr, uncomprLen)
    d_stream = Z_stream.new
    uncompr[0,7] = "garbage"

    d_stream.next_in  = Bytef.new(compr)
    d_stream.avail_in = comprLen

    err = inflateInit(d_stream)
    CHECK_ERR(err, "inflateInit")
    d_stream.next_out = Bytef.new(uncompr)
    d_stream.avail_out = uncomprLen

    while true
        err = inflate(d_stream, Z_NO_FLUSH)
        break if (err == Z_STREAM_END)
        if (err == Z_NEED_DICT)
            if (d_stream.adler != @@dictId)
                puts("unexpected dictionary")
                exit(1)
            end
            err = inflateSetDictionary(d_stream, @@dictionary,@@dictionary.length)

        end
        CHECK_ERR(err, "inflate with dict")
    end

    err = inflateEnd(d_stream)
    CHECK_ERR(err, "inflateEnd")
    uncompr = uncompr[0,d_stream.total_out]
    if uncompr != @@hello
        puts("bad inflate with dict")
        exit(1)
    else
        puts("inflate with dictionary: #{uncompr}")
    end
end

    comprLen = 10000*4
    uncomprLen = comprLen
    myVersion = ZLIB_VERSION

    if (zlibVersion[0] != myVersion[0])
        puts("incompatible zlib version")
        exit(1)
    elsif (zlibVersion != ZLIB_VERSION)
        puts("warning: different zlib version")
    end

    compr    = 0.chr * comprLen
    uncompr  = 0.chr * uncomprLen
    if (compr.nil? || uncompr.nil?)
        puts("out of memory")
        exit(1)
    end
    test_compress(compr, comprLen, uncompr, uncomprLen)

    test_gzio((ARGV.length > 0 ? ARGV[0] : TESTFILE),
              uncompr, uncomprLen)

    test_deflate(compr, comprLen)
    test_inflate(compr, comprLen, uncompr, uncomprLen)
    test_large_deflate(compr, comprLen, uncompr, uncomprLen)
    test_large_inflate(compr, comprLen, uncompr, uncomprLen)

    test_flush(compr, comprLen)

    test_sync(compr, comprLen, uncompr, uncomprLen)
    comprLen = uncomprLen

    test_dict_deflate(compr, comprLen)
    test_dict_inflate(compr, comprLen, uncompr, uncomprLen)



