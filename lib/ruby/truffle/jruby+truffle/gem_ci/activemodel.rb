subdir 'activemodel'
repository_name 'rails'

git_clone 'https://github.com/rails/rails.git' unless File.exists? repository_dir
git_checkout git_tag('4.2.6')

use_only_https_git_paths!

has_to_succeed setup

result run(%w[--require-pattern test/**/*_test.rb -r exclude_tests -- -I test -e nil], raise: false)

