require 'erb'
require 'jruby/util'

# Calculate relative basedir
require 'pathname'
rb_path = Pathname.new(maven["classpath_rb"]).expand_path
basedir = Pathname.new(maven["basedir"].gsub("\\", "/")).relative_path_from(rb_path).to_s

# Substitute HOME and BASEDIR in the classpath
maven["classpath"] = maven["classpath"].gsub(maven["basedir"], '#{BASEDIR}')
maven["classpath"] = maven["classpath"].gsub(ENV["HOME"], '#{ENV[\'HOME\']}')
classpath = maven["classpath"].split(File::PATH_SEPARATOR)

File.open(rb_path, 'w') do |f|
  template = JRuby::Util.classloader_resources("classpath.erb").first
  raise "classpath.rb template not found" unless template
  erb = ERB.new(File.read(template))
  f << erb.result(binding)
end
