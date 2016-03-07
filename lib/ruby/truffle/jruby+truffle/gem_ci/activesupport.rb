subdir 'activesupport'
repository_name 'rails'

unless File.exists? repository_dir
  git_clone 'https://github.com/rails/rails.git', branch: '4-2-stable'
end

use_only_https_git_paths!

has_to_succeed setup

result run(%w[--require-pattern test/**/*_test.rb -r exclude_tests -- -I test -e nil], raise: false)

