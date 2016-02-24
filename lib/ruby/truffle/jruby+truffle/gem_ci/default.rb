unless File.exists? repository_dir
  git_clone option(:git)
end

setup

result run(%w[-S rake], raise: false)

