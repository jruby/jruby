task :installer do
  ant "dist" unless ENV['TESTING_INSTALLER']
  sh "#{INSTALL4J_EXECUTABLE} -m win32 -D jruby.version=#{VERSION_JRUBY} " \
     "install/jruby.install4j" do |ok, result|
    $stderr.puts "** Something went wrong: #{result}" unless ok
  end
  Dir["#{BUILD_DIR}/installers/*.exe"].each do |file|
    md5_checksum file
    sha1_checksum file
  end
end
