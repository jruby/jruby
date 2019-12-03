require 'erb'
require 'fileutils'
require 'rbconfig'

include FileUtils

INSTALL4J_CONFIG_FILE = File.join(File.dirname(__FILE__), '..', 'install', 'jruby.install4j')

task :installer => [:windows_installer]

task :windows_installer => :init_release do
  version = jruby_version
  unpacked_dir = unpack_binary_distribution(version)
  
  install_windows_gems(unpacked_dir)

  if File.executable?(INSTALL4J_EXECUTABLE)
    root_dir = Dir.pwd
    Dir.chdir(unpacked_dir) do
      sh %Q^"#{INSTALL4J_EXECUTABLE}" -m windows -D jruby.dist.location=#{root_dir},jruby.location=#{unpacked_dir},ruby.version=#{VERSION_RUBY},jruby.version=#{version},ruby.patchlevel=0,ruby.buildplatform=i386-mingw32 #{INSTALL4J_CONFIG_FILE}^ do |ok, result|
        $stderr.puts "** Something went wrong: #{result}" unless ok
      end
      mv Dir[File.join(root_dir, 'install', '*.exe')], File.join(root_dir, RELEASE_DIR)
      Dir[File.join(RELEASE_DIR, '*.exe')].each do |file|
        md5_checksum file
        sha1_checksum file
      end
    end
  else
    puts "Skipping windows installers since install4j is not available"
  end
end

task :init_release do
  mkdir_p RELEASE_DIR
end

def unpack_binary_distribution(jruby_version)
  require 'tmpdir'
  dist_file = Dir[File.join(DIST_FILES_DIR, "jruby-dist-*-bin.zip")][0]

  unless File.exist?(dist_file)
    raise ArgumentError.new "No binary distribution file: '#{dist_file}'."
  end

  dir = Dir.mktmpdir
  sh "unzip -q -o #{dist_file} -d #{dir}"
  puts "unziped into #{File.join dir, "jruby-#{jruby_version}"}"
  puts "sh ls #{File.join dir, "jruby-#{jruby_version}"}"
  File.join dir, "jruby-#{jruby_version}"
end

def install_windows_gems(unpacked_dir)
  sh "#{File.join(unpacked_dir, 'bin', 'jruby')} -S gem install #{INSTALLER_GEMS}"
end
