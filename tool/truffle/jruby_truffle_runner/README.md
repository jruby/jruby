# JRuby+Truffle Runner

`jruby+truffle_runner` gem is a small command line utility designed to run and test Ruby gems and applications 
on JRuby+Truffle Ruby runtime. It uses other Ruby implementation to prepare environment and to execute files, tests 
on JRuby+Truffle. It is a temporary tool to make working with JRuby+Truffle easy until it fully supports 
`rubygems` and `bundler`

## Installation

There are 3 options.

1.  Install the gem from rubygems.org (Not yet published).
    Install the gem `gem install jruby+truffle_runner`
2.  Use `jt` tool from JRuby's repository.
    Run `jt install-tool` to install the gem from the cloned repository.
3.  Create alias for the gem's executable. E.g. add 
    `alias jruby+truffle="~/path-to-jruby/tool/truffle/jruby_truffle_runner/bin/jruby+truffle" to your `.zshrc`.

Then run `jruby+truffle --help` to see available subcommands.

## Setup

There is a `setup` subcommand to create environment for JRuby+Truffle.

-   Go to directory of a gem/application you would like to test.
-   Run `jruby+truffle setup`

It copies default configuration (part of the gem) if one is available for a given gem (looks for a `gemspec` in current 
directory). It installs all required gems (based on `Gemfile`) to local bundle (default path: `.jruby+truffle_bundle`), 
and executes other steps defined in configuration file (`.jruby+truffle.yaml`) or as command line options 
(see `jruby+truffle setup --help` to learn what additional setup steps are available, or see one of the bundled 
configuration files found in `gem_configurations` directory).

## Running

After the environment is set the gem can be used to execute code, files, or gem's executables on JRuby+Truffle 
in prepared environment. Examples follows (options after `--` are interpreted by Ruby, options before `--` are options 
for this tool):

-   `jruby+truffle run -- file.rb` - executes file.rb 
-   `jruby+truffle run -- -e '1+1'` - evaluates 1+1 expresion
-   `jruby+truffle run -- -I test test/a_test_file_test.rb` - runs test_file
-   `jruby+truffle run -S rspec -- spec/a_spec_file_spec.rb` - runs a spec file using executable of rspec gem
-   `jruby+truffle run --require mocks -- file.rb` - executes file.rb, but requires mocks first. 
    (mocks can be made to load always by putting the option to configuration file instead)

See `jruby+truffle run --help` to see all available options.

## Clean up

To remove all files added during setup phase run `jruby+truffle clean`, it'll only keep `.jruby+truffle.yaml` 
configuration file for future re-setup. 

