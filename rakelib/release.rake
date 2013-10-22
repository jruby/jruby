
DIST_FILES_GLOB = "jruby-dist-*.{zip,gz}"
DIST_FILES_DIR = File.join('maven', 'jruby-dist', 'target')
JRUBY_COMPLETE_DIR = File.join('maven', 'jruby-complete', 'target')
JRUBY_COMPLETE_GLOB = 'jruby-complete-*.jar'
JRUBY_JARS_DIR = File.join('maven', 'jruby-jars', 'pkg')
JRUBY_JARS_GLOB = 'jruby-jars-*.gem'

desc "post-process mvn build to generate properly named and fingerprinted files"
task :post_process_artifacts => :windows_installer do
  cp Dir[File.join(DIST_FILES_DIR, DIST_FILES_GLOB)], RELEASE_DIR
  Dir[File.join(RELEASE_DIR, DIST_FILES_GLOB)].each do |file|
    real_file = file.sub(/-dist/, '')
    if real_file =~ /-bin/
      real_file = real_file.sub(/-bin/, '').sub(/-/, '-bin-')
    elsif real_file =~ /-src/
      real_file = real_file.sub(/-src/, '').sub(/-/, '-src-')
    end
    mv file, real_file
    md5_checksum real_file
    sha1_checksum real_file
  end

  cp Dir[File.join(JRUBY_COMPLETE_DIR, JRUBY_COMPLETE_GLOB)], RELEASE_DIR
  Dir[File.join(RELEASE_DIR, JRUBY_COMPLETE_GLOB)].each do |file|
    md5_checksum file
    sha1_checksum file
  end

  cp Dir[File.join(JRUBY_JARS_DIR, JRUBY_JARS_GLOB)], RELEASE_DIR
  Dir[File.join(RELEASE_DIR, JRUBY_JARS_GLOB)].each do |file|
    md5_checksum file
    sha1_checksum file
  end
end

# Assume there will only be one release performed after a clean so
# we don't have multiple artifacts in target.
def jruby_version
  Dir[File.join(DIST_FILES_DIR, "jruby-dist-*.zip")].each do |f|
    return $1 if f =~ /jruby-dist-(.*)-bin.zip/
  end
  raise ArgumentError "mvn release:prepare has not been run"
end
