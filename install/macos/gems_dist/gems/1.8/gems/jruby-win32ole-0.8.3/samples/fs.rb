require 'win32ole'

def showDriveType(drive)
  case drive.DriveType
  when 1 then # DriveTypeRemovable 
    "Removable"
  when 2 then # DriveTypeFixed
    "Fixed"
  when 3 then # DriveTypeNetwork
    "Network"
  when 4 then # DriveTypeCDROM
    "CD-ROM"
  when 5 then # DriveTypeRAMDick
    "RAM Disk"
  else
    "Uknown #{drive.DriveType}"
  end
end

fso = WIN32OLE.new('Scripting.FileSystemObject')
drives = fso.Drives

puts "Number of Drives: #{drives.Count}"
puts ""
puts [''    ,     '',  'Drive', 'File', 'Total', 'Free', 'Avail.', 'Serial'].join("\t")
puts ['Path', 'Type', 'Ready?', 'Name', 'Space', 'Space', 'Space', 'Number'].join("\t")
100.times { printf "-" }
puts ""

drives.each do |drive|
  printf [drive.Path, showDriveType(drive), drive.IsReady].join("\t")
  if drive.IsReady
    if drive.DriveType == 3 # DriveTypeNetwork
      printf "\t#{drive.ShareName}\t"
    else
      printf "\t#{drive.VolumeName}\t"
    end
    printf [drive.FileSystem, drive.TotalSize, drive.FreeSpace, drive.AvailableSpace, drive.SerialNumber].join("\t")
  end
  puts ""
end

dir = 'C:/opt'
puts ""
puts "#{dir} contents"
folder = fso.GetFolder(dir)
folder.files.each do |file|
  puts file
end

