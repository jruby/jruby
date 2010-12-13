desc "Build the jruby-jars gem"
task :gem do
  ruby "-S", "maybe_install_gems", "hoe"
  Dir.chdir("gem") do
    ruby "-S", "rake", "package"
  end
end
