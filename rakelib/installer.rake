require 'erb'
require 'fileutils'
require 'rbconfig'

include FileUtils

POSTFLIGHT = 'scripts/installer.postflight'
PMDOC = 'JRuby-installer.pmdoc/01jruby.xml'
GEMSPMDOC = 'JRuby-installer.pmdoc/02gems.xml'
GEMSMAC = 'install/macos/rubygems/jruby_mac.rb'
JRUBY_DEST = '/Library/Frameworks/JRuby.framework'

UNINSTALLER_INDEX = 'JRuby-uninstaller.pmdoc/index.xml'
UNINSTALLER_PMDOC = 'JRuby-uninstaller.pmdoc/01uninstaller.xml'
UNINSTALLER_SCRIPT = 'scripts/uninstaller.postinstall'
UNINSTALLER_WELCOME= 'Welcome.uninstaller.rtf'

task :installer => [:macos_installer, :windows_installer]

task :macos_installer do
  next unless RbConfig::CONFIG['target_os'] =~ /darwin/

  pkgmaker = `mdfind "kMDItemDisplayName=='PackageMaker*'"`.chomp
  if pkgmaker == ""
    warn 'PackageMaker not found, skipping OS X Installer'
    next
  else
    pkgmaker=File.join(pkgmaker, 'Contents', 'MacOS', 'PackageMaker')
  end

  puts "\nBuilding OS X Installer"

  cleanup

  raise "JRuby #{VERSION_JRUBY} dist ZIP not found!" if !File.exist?(DIST_ZIP)
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
    sh "time #{pkgmaker} --no-recommend -v --doc JRuby-installer.pmdoc --out #{PKG_DIR}/JRuby-#{VERSION_JRUBY}.pkg --version #{VERSION_JRUBY}"
    sh "time #{pkgmaker} --no-recommend -v --doc JRuby-uninstaller.pmdoc --out #{PKG_DIR}/JRuby-uninstaller-#{VERSION_JRUBY}.pkg --version #{VERSION_JRUBY}"

    rm DMG if File.exist? DMG = File.join(BASE_DIR, DIST_DIR, "JRuby-#{VERSION_JRUBY}.dmg")
    sh "time hdiutil create #{DMG} -volname JRuby-#{VERSION_JRUBY} -fs HFS+ -srcfolder #{PKG_DIR}"

    cleanup
  end
end

task :windows_installer => :install_installer_gems do
  if File.executable?(INSTALL4J_EXECUTABLE)
    sh "\"#{INSTALL4J_EXECUTABLE}\" -m win32 -D ruby.version=#{VERSION_RUBY},jruby.version=#{VERSION_JRUBY},ruby.patchlevel=#{VERSION_RUBY_PATCHLEVEL},ruby.buildplatform=i386-mingw32 install/jruby.install4j" do |ok, result|
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

#           #
#  HELPERS  #
#           #

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
