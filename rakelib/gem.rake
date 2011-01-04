desc "Build the jruby-jars gem"
task :gem do
  ruby "-S", "maybe_install_gems", "hoe"
  Dir.chdir("gem") do
    ruby "-S", "rake", "package"
    cp FileList["pkg/*.gem"], "../#{DIST_DIR}"
  end
  Dir["#{DIST_DIR}/*.gem"].each do |file|
    md5_checksum file
    sha1_checksum file
  end
end
