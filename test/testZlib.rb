require 'test/minirunit'
require 'zlib'

s = "test comression string"
[Zlib::NO_COMPRESSION,
 Zlib::BEST_SPEED,
 Zlib::BEST_COMPRESSION,
 Zlib::DEFAULT_COMPRESSION].each do |level|
  test_equal(s, Zlib::Inflate.inflate(Zlib::Deflate.deflate(s, level)))
end

filename = "____temp_zlib_file"

Zlib::GzipWriter.open(filename) { |z| z.puts 'HEH' }
Zlib::GzipReader.open(filename) { |z| test_equal("HEH\n", z.gets) }

z = Zlib::GzipWriter.open(filename)
z.puts 'HOH'
z.puts 'foo|bar'
z.close

z = Zlib::GzipReader.open(filename)
test_equal("HOH\n", z.gets)
test_equal("foo|", z.gets("|"))
test_equal("bar\n", z.gets)
z.close

File.unlink(filename)
