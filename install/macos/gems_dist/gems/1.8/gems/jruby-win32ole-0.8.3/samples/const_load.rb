require 'win32ole'

module FOO
end

ie = WIN32OLE.new('InternetExplorer.Application')
WIN32OLE.const_load(ie, FOO)
FOO.constants.each do |c|
  puts "#{c}: #{FOO.const_get(c)}"
end
