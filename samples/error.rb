
require 'java'

Java::define_exception_handler "java.lang.NumberFormatException" do |e|
  puts e.java_type
  puts JavaClass.for_name(e.java_type).java_method(:getMessage).invoke(e)
end

module JavaLang
  include_package 'java.lang'
end

JavaLang::Long.parseLong("23aa")
