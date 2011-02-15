require 'erb'
require 'jruby/util'

classpath = maven["classpath"].split(File::PATH_SEPARATOR)
File.open(maven["classpath_rb"], 'w') do |f|
  template = JRuby::Util.classloader_resources("classpath.erb").first
  raise "classpath.rb template not found" unless template
  erb = ERB.new(File.read(template))
  f << erb.result(binding)
end
