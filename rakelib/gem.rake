desc "Build the jruby-jars gem"
namespace :gem do
  task 'jruby-jars' do
    rdoc_gem = Dir.glob("target/rdoc-*.gem").sort.last
    ruby "-S", "maybe_install_gems", rdoc_gem
    Dir.chdir("gems/jruby-jars") do
      ruby "-S", "rake", "package"
      cp FileList["pkg/*.gem"], "../../#{DIST_DIR}"
    end
    Dir["#{DIST_DIR}/*.gem"].each do |file|
      md5_checksum file
      sha1_checksum file
    end
  end
  task 'jruby-openssl' do
    rdoc_gem = Dir.glob("target/rdoc-*.gem").sort.last
    ruby "-S", "maybe_install_gems", rdoc_gem
    Dir.chdir("gems/jruby-openssl") do
      ruby "-S", "rake", "package"
      cp FileList["pkg/*.gem"], "../../#{DIST_DIR}"
    end
    Dir["#{DIST_DIR}/*.gem"].each do |file|
      md5_checksum file
      sha1_checksum file
    end
  end
end
