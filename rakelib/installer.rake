task :installer => :install_installer_gems do
  if File.executable?(INSTALL4J_EXECUTABLE)
    sh "\"#{INSTALL4J_EXECUTABLE}\" -m win32 -D jruby.version=#{VERSION_JRUBY} install/jruby.install4j" do |ok, result|
      $stderr.puts "** Something went wrong: #{result}" unless ok
    end
    mv Dir["#{BUILD_DIR}/installers/*.exe"], DIST_DIR
    Dir["#{DIST_DIR}/*.exe"].each do |file|
      md5_checksum file
      sha1_checksum file
    end
  else
    puts "Skipping windows installers since install4j is not available"
  end
end
