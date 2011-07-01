require 'win32ole'

ie = WIN32OLE.new('InternetExplorer.Application')
vis = ie.ole_method_help('Visible')
puts "VISIBLE: #{ie.visible}"
puts "VISIBLE w/ []: #{ie['Visible']}"
puts "VISIBLE w/ Invoke: #{ie.invoke('Visible')}"
ie._setproperty(vis.dispid, [true], [true])
ie.Visible = TRUE  # Upper-case
puts "VISIBLE: #{ie.visible}"
sleep 4
ie['Visible'] = false
puts "VISIBLE: #{ie.visible}"
#puts ie.ole_methods
ie.gohome
puts "NAME: #{ie.name}"  # Lower-case
ie.quit 



