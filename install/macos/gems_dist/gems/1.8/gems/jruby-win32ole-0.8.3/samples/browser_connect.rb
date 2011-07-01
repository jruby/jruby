require 'win32ole'

# Something tells me this just current browser and not specifically ie
ie = WIN32OLE.connect '{c08afd90-f2a1-11d1-8455-00a0c91f3880}'
ie.gohome
sleep 1
puts "NAME: #{ie.name}"  # Lower-case
ie.quit 


