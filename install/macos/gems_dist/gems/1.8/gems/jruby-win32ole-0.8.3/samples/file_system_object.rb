require 'win32ole'

fso = WIN32OLE.new('Scripting.FileSystemObject')
folder = fso.GetFolder('C:/') 
folder.SubFolders.each { |file| puts file.Path + '\\' }
folder.Files.each { |file| puts file.Path }

