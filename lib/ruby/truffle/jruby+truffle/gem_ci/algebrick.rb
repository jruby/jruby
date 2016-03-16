git_clone 'https://github.com/pitr-ch/algebrick.git' unless File.exists? repository_dir
git_checkout git_tag '0.7.3'

has_to_succeed setup

result run(%w[test/algebrick_test.rb], raise: false)

