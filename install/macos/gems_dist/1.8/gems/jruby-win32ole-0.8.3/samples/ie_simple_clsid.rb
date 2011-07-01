require 'win32ole'

# Something tells me this just current browser and not specifically ie
ie = WIN32OLE.new '{c08afd90-f2a1-11d1-8455-00a0c91f3880}'
puts "VISIBLE: #{ie.visible}"
ie.Visible = TRUE  # Upper-case
puts "VISIBLE: #{ie.visible}"
sleep 1
#ie.gohome
puts "NAME: #{ie.name}"  # Lower-case
ie.quit 


