
require 'java'

Java::define_exception_handler "java.lang.NumberFormatException" do |e|
  puts e.java_type
  puts e.java_class.java_method(:getMessage).invoke(e)
end

module JavaLang
  include_package 'java.lang'
end

JavaLang::Long.parseLong("23aa")
