desc "Build the jruby-jars gem"
task :gem do
  Dir.chdir("gem") do
    ruby "-S", "rake", "package"
  end
end
