declare_options git:     ['--git URL', 'Path to the gem\'s repository', STORE_NEW_VALUE, nil],
                version: ['--version VERSION', 'Version of the gem', STORE_NEW_VALUE, nil]

git_clone option(:git) unless File.exists? repository_dir
git_checkout git_tag option(:version)

delete_gemfile_lock!
use_only_https_git_paths!

has_to_succeed setup

result run(%w[-S rake], raise: false)

