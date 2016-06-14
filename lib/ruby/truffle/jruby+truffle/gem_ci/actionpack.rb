declare_options exclude: ['--[no-]exclude',
                          'Exclude known failing tests',
                          STORE_NEW_VALUE,
                          true]

subdir 'actionpack'
repository_name 'rails'

git_clone 'https://github.com/rails/rails.git' unless File.exists? repository_dir
git_checkout git_tag('4.2.6')

use_only_https_git_paths!

has_to_succeed setup

set_result run([%w[--require-pattern test/**/*_test.rb],
            (option(:exclude) ? %w[-r excluded-tests] : []),
            %w[-- -I test -e nil]].flatten(1),
           raise: false)

