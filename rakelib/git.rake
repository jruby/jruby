def git_repo_exists?(dir)
  File.exists? File.join(dir, ".git")
end

def git_shallow_clone(label, git_repository, local_directory)
  puts "No #{label} repo found: cloning to #{local_directory}"
  rm_rf local_directory  # Something there, but not a git repo.  Destroy!
  sh "git clone --depth 1 #{git_repository} #{local_directory}"
  Dir.chdir(local_directory) { yield } if block_given?
end

def git_pull(label, local_directory)
  puts "#{label} repo found: updating repo at #{local_directory}"
  sh "git pull"
  Dir.chdir(local_directory) { yield } if block_given?
end
