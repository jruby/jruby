Java::define_exception_handler "java.lang.NumberFormatException" do |e|
  puts e.type
  puts e.message
end

Java::Lang::Integer.parseInt "23aa"
