def git_repo_exists?(dir)
  File.exists? File.join(dir, ".git")
end

def git_shallow_clone(label, git_repository, local_directory)
  git_clone(label, git_repository, local_directory, true)
end

def git_clone(label, git_repository, local_directory, shallow = false)
  puts "No #{label} repo found: cloning to #{local_directory}"
  rm_rf local_directory  # Something there, but not a git repo.  Destroy!
  cmd = "git clone #{git_repository} #{local_directory}"
  cmd << " --depth 1" if shallow
  sh cmd
  Dir.chdir(local_directory) { yield } if block_given?
end

def git_simple_command(command, label, local_directory, ignore_error = false)
  puts "#{label} repo found: `git #{command}` repo at #{local_directory}"
  Dir.chdir(local_directory) do
    begin
      sh "git #{command}"
    rescue Exception => e
      unless ignore_error
        raise e
      else
        puts "git #{command} failed, but ignored: #{e.message}"
      end
    end
    yield if block_given?
  end 
end

def git_pull(label, local_directory, ignore_error = false)
  git_simple_command("pull", label, local_directory, ignore_error)
end

def git_fetch(label, local_directory, ignore_error = false)
  git_simple_command("fetch", label, local_directory, ignore_error)
end

def git_checkout(label, tag, local_directory)
  git_simple_command("checkout -q #{tag}", label, local_directory)
end

def git_move_to_head_detached(label, tag, local_directory)
  git_simple_command('checkout -q `git rev-parse HEAD`', label, local_directory)
end

def git_submodule_update(path, ignore_error = false)
  begin
    sh "git submodule update --init #{path}"
  rescue Exception => e
    unless ignore_error
      raise e
    else
      puts "git #{command} failed, but ignored: #{e.message}"
    end
  end
end
