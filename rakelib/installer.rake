require 'erb'
require 'fileutils'
require 'rbconfig'

include FileUtils

# Dummy value for now
MAC_DIST='jruby'

POSTFLIGHT = 'scripts/installer.postflight'
PMDOC = 'JRuby-installer.pmdoc/01jruby.xml'
GEMSPMDOC = 'JRuby-installer.pmdoc/02gems.xml'
GEMSMAC = 'install/macos/rubygems/jruby_mac.rb'
JRUBY_DEST = '/Library/Frameworks/JRuby.framework'
DIST_DIR= ENV['DIST_DIR'] || '.'
INSTALL4J_CONFIG_FILE = File.join(File.dirname(__FILE__), '..', 'install', 'jruby.install4j')

UNINSTALLER_INDEX = 'JRuby-uninstaller.pmdoc/index.xml'
UNINSTALLER_PMDOC = 'JRuby-uninstaller.pmdoc/01uninstaller.xml'
UNINSTALLER_SCRIPT = 'scripts/uninstaller.postinstall'
UNINSTALLER_WELCOME= 'Welcome.uninstaller.rtf'

task :installer => [:macos_installer, :windows_installer]

task :macos_installer do
  version = jruby_version

  next unless RbConfig::CONFIG['target_os'] =~ /darwin/
  if `uname -r`.to_f >= 12 # Darwin 12 = "Mountain Lion"
    Dir.chdir "#{BASE_DIR}/dist" do
      pkg_file = "jruby-#{version}.pkg"
      puts "Building Mountain Lion package"
      sh "pkgbuild --identifier org.jruby.pkg --install-location /Applications/jruby \\
        --version #{version} --root jruby-bin-#{version} #{pkg_file}"
      sh "md5 -q #{pkg_file} > #{pkg_file}.md5"
      sh "openssl sha1 #{pkg_file} | cut -f2 -d' ' > #{pkg_file}.sha1"
    end
    next
  end

  pkgmaker_dirs = `mdfind "kMDItemDisplayName=='PackageMaker*'"`.chomp.split
  pkgmaker_apps = pkgmaker_dirs.map{|d| File.join(d, 'Contents', 'MacOS', 'PackageMaker')}.select{|f| File.exists? f}
  if pkgmaker_apps.empty?
    warn 'PackageMaker not found, skipping OS X Installer'
    next
  elsif pkgmaker_apps.size > 1
    warn "\nMultiple PackageMaker apps found, skipping OS X Installer:\n#{pkgmaker_apps.join("\n")}\n"
    next
  end

  pkgmaker=pkgmaker_apps[0]

  puts "\nBuilding OS X Installer"

  cleanup

  raise "JRuby #{version} dist ZIP not found!" if !File.exist?(DIST_ZIP)
  sh "unzip -o #{DIST_ZIP} -d #{BUILD_DIR}"

  prepare_rubygems

  Dir.chdir "#{BASE_DIR}/install/macos" do

    puts "- Setting package version"
    replace_variables_in POSTFLIGHT
    replace_variables_in PMDOC
    replace_variables_in GEMSPMDOC
    replace_variables_in UNINSTALLER_INDEX
    replace_variables_in UNINSTALLER_PMDOC
    replace_variables_in UNINSTALLER_SCRIPT
    replace_variables_in UNINSTALLER_WELCOME

    puts "- Building package"
    mkdir_p PKG_DIR
    sh "time #{pkgmaker} --no-recommend -v --doc JRuby-installer.pmdoc --out #{PKG_DIR}/JRuby-#{version}.pkg --version #{version}"
    sh "time #{pkgmaker} --no-recommend -v --doc JRuby-uninstaller.pmdoc --out #{PKG_DIR}/JRuby-uninstaller-#{version}.pkg --version #{version}"

    rm DMG if File.exist? DMG = File.join(BASE_DIR, DIST_DIR, "JRuby-#{version}.dmg")
    sh "time hdiutil create #{DMG} -volname JRuby-#{version} -fs HFS+ -srcfolder #{PKG_DIR}"

    cleanup
  end
end

task :windows_installer => :init_release do
  version = jruby_version
  unpacked_dir = unpack_binary_distribution(version)
  
  install_windows_gems(unpacked_dir)

  if File.executable?(INSTALL4J_EXECUTABLE)
    root_dir = Dir.pwd
    Dir.chdir(unpacked_dir) do
      sh %Q^"#{INSTALL4J_EXECUTABLE}" -m win32 -D jruby.dist.location=#{root_dir},jruby.location=#{unpacked_dir},ruby.version=#{VERSION_RUBY},jruby.version=#{version},ruby.patchlevel=0,ruby.buildplatform=i386-mingw32 #{INSTALL4J_CONFIG_FILE}^ do |ok, result|
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

#           #
#  HELPERS  #
#           #

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

def replace_variables_in(path)
  File.open(path,"w") do |f|
    f.write ERB.new(File.read("#{path}.erb")).result
  end
end

def prepare_rubygems
  replace_variables_in GEMSMAC
  cp GEMSMAC, GEMS_DEFAULTS_DIR

  File.open("#{GEMS_DEFAULTS_DIR}/jruby.rb", "a+") do |file|
    file.write("require 'rubygems/defaults/jruby_mac'")
  end

  mv "#{MAC_DIST}/lib/ruby/gems", GEMS_DIST_DIR
end

def cleanup
  puts "- Cleaning directories"
  [MAC_DIST, GEMS_DIST_DIR, PKG_DIR ].each do |f|
    rm_r f if File.exist? f
  end
end
