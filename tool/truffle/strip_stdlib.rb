# Deletes files form stdlib-2.2.2 directory which are not used by JRuby+Truffle.

require 'pathname'
require 'fileutils'
require 'pp'

jruby_root     = Pathname(__FILE__).dirname.parent.parent
stdlib         = jruby_root.join 'lib', 'ruby', 'truffle', 'stdlib'
truffle_stdlib = jruby_root.join 'lib', 'ruby', 'truffle', 'mri'

stdlib_files = Dir.glob stdlib.join('**', '*')

needed_files = Dir.glob(truffle_stdlib.join('**', '*')).reduce([]) do |arr, file|
  file = Pathname(file)
  next arr if File.directory? file
  content = File.read(file)
  unless content =~ /^require_relative ('|")([^'"]*)('|")( ?\+ ?File\.basename\(__FILE__\))?/
    puts "File #{file} has unmatched content: #{content.inspect}"
    next arr
  end

  needed_file = file.dirname.join($2).expand_path
  needed_file = needed_file.dirname unless $4
  needed_file = needed_file.join(file.basename)

  a = file.relative_path_from(truffle_stdlib)
  b = needed_file.relative_path_from(stdlib)
  puts "File: #{file} requires wrong file from stdlib: #{needed_file}" if a != b

  arr << needed_file.to_s
  arr
end

# pp needed_files

to_delete    = stdlib_files - needed_files
to_delete.delete_if { |f| File.directory?(f) || f =~ /README\.md/ }

if ARGV[0] == 'delete'

  to_delete.each { |f| FileUtils.rm f, verbose: true if File.file? f }

  to_delete.each do |f|
    next if File.file? f
    Dir.delete f rescue nil # keep non-empty dirs
  end
else
  pp to_delete
end
