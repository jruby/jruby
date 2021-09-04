# This tool scans our src dir for "package.rb" files and evals
# them, using the output for "package.html" in the same dir. It
# gets run for the "apidocs" Ant target.

puts "Generating package.html files from package.rb..."
Dir['src/**/package.rb'].each do |package_rb|
  puts "  #{package_rb}"
  package_html = eval File.read(package_rb)
  filename = File.join(File.dirname(package_rb), "package.html")
  File.open(filename, 'w') do |file|
    file.write(package_html)
  end
end