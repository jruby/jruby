def git_repo_exists?(dir)
  File.exists? File.join(dir, ".git")
end

def git_shallow_clone(label, git_repository, local_directory)
  puts "No #{label} repo found: cloning to #{local_directory}"
  rm_rf local_directory  # Something there, but not a git repo.  Destroy!
  sh "git clone --depth 1 #{git_repository} #{local_directory}"
  Dir.chdir(local_directory) { yield } if block_given?
end

def git_simple_command(command, label, local_directory)
  puts "#{label} repo found: `git #{command}` repo at #{local_directory}"
  Dir.chdir(local_directory) do 
    sh "git #{command}"
    yield if block_given?
  end 
end

def git_pull(label, local_directory)
  git_simple_command("pull", label, local_directory)
end

def git_fetch(label, local_directory)
  git_simple_command("fetch", label, local_directory)
end

def git_checkout(label, tag, local_directory)
  git_simple_command("checkout #{tag}", label, local_directory)
end
