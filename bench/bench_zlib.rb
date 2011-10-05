require 'benchmark'
require 'zlib'
require 'stringio'

FILES = Dir.glob(File.expand_path('*.rb', File.dirname(__FILE__)))

Benchmark.bmbm do |bm|
  bm.report('Zlib::Deflate') do
    z = Zlib::Deflate.new
    300.times do
      FILES.each do |file|
        z << File.read(file)
      end
      z.flush
    end
    z.finish
  end

  bm.report('Zlib::Inflate') do
    # prepare
    z = Zlib::Deflate.new
    FILES.each do |file|
      z << File.read(file)
    end
    src = z.finish

    3000.times do
      z = Zlib::Inflate.new
      z << src
      z.finish
    end
  end

  bm.report('Zlib::GzipWriter') do
    s = StringIO.new
    Zlib::GzipWriter.wrap(s) do |gz|
      300.times do
        FILES.each do |file|
          gz.write File.read(file)
        end
        s.truncate(0)
      end
    end
  end

  bm.report('Zlib::GzipReader') do
    # prepare
    s = StringIO.new
    Zlib::GzipWriter.wrap(s) do |gz|
      FILES.each do |file|
        gz.write File.read(file)
      end
    end
    src = s.string

    3000.times do
      Zlib::GzipReader.wrap(StringIO.new(src)) do |gz|
        gz.read
      end
    end
  end
end
