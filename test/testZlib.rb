require 'test/minirunit'
require 'zlib'

filename = "____temp_zlib_file"

Zlib::GzipWriter.open(filename) { |z| z.puts 'HEH' }
Zlib::GzipReader.open(filename) { |z| test_equal("HEH\n", z.gets) }

z = Zlib::GzipWriter.open(filename)
z.puts 'HOH'
z.close

z = Zlib::GzipReader.open(filename)
test_equal("HOH\n", z.gets)
z.close

File.unlink(filename)
