namespace :maven do
  desc "Update versions in maven poms with string passed in ENV['VERSION']"
  task :updatepoms do
    version = ENV['VERSION'] or abort("Pass the new version with VERSION={version}")
    system "mvn versions:set -DnewVersion=#{version}"
  end
end
