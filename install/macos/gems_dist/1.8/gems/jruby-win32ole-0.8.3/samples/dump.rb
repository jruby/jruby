require 'win32ole'

name = proc {|a, b| a.name <=> b.name }
WIN32OLE_TYPELIB.typelibs.sort(&name).each do |typelib|
  puts "TYPELIB: #{typelib.name}"
  typelib.ole_classes.sort(&name).each do |ole_class|
    puts "  CLASS: #{ole_class.name}"
    puts "   GUID   : #{ole_class.guid}"
    puts "   PROGID : #{ole_class.progid}" 
    puts "   DESCR  : #{ole_class.helpstring}"

    ole_class.ole_methods.sort(&name).each do |ole_method|
      print "    #{ole_method.return_type} #{ole_method.name}("
      print ole_method.params.to_a.inject([]) { |sum, param|
        sum << "#{param.ole_type} #{param.name}"
      }.join(", ")
      puts ")"
    end
  end
end
