# JRuby+Truffle Runner

`jruby-truffle-tool` is a small command line utility designed to run and
test Ruby gems and applications on the JRuby+Truffle Ruby runtime. It uses another
Ruby implementation to prepare the environment and executes files and tests
on JRuby+Truffle. It is a temporary tool to make working with JRuby+Truffle
easy until it fully supports `rubygems` and `bundler`

## Installation

The command line tool is part of the JRuby distribution. It is available in
JRuby's `bin` directory.

Just run `jruby-truffle-tool --help` to see the available subcommands.

## Setup

There is a `setup` subcommand to create the environment for JRuby+Truffle.

-   Go to a directory of a gem/application you would like to test.
-   Run `jruby-truffle-tool setup`

It uses the default configuration (part of the tool) if one is available for a
given gem (it looks for a `gemspec` in current directory). It installs all
required gems (based on the `Gemfile`) to a local bundle directory (default path:
`.jruby-truffle-tool_bundle`), and executes other steps defined in the configuration
files or as command line options (see `jruby-truffle-tool
setup --help` to learn what additional setup steps are available, or see one of
the default configuration files found in `gem_configurations` directory). After
it finishes, the `run` subcommand can be used.

## Running

After the environment is set the gem can be used to execute code, files, or
gem's executables on JRuby+Truffle in the prepared environment. Examples follows
(options after `--` are interpreted by Ruby, options before `--` are options
for this tool):

-   `jruby-truffle-tool run -- file.rb` - executes file.rb
-   `jruby-truffle-tool run -- -e '1+1'` - evaluates 1+1 expression
-   `jruby-truffle-tool run -- -I test test/a_test_file_test.rb` - runs a test file
-   `jruby-truffle-tool run -S rspec -- spec/a_spec_file_spec.rb` - runs a spec file
    using the `rspec` executable of the rspec gem
-   `jruby-truffle-tool run --require mocks -- file.rb` - executes file.rb, but
    requires mocks first. (mocks can be made to load always by putting the
    option to configuration file (`.jruby-truffle-tool.yaml`) instead)

See `jruby-truffle-tool run --help` to see all available options.

## Clean up

To remove all files added during the setup phase, run `jruby-truffle-tool clean`,
it will only keep the `.jruby-truffle-tool.yaml` configuration file for future re-setup.

## Pre-configuration

Options which are always required or are part of the setup step can
pre-configured in the default configuration (included in the tool) or in the local 
`.jruby-truffle-tool.yaml` configuration file to avoid repeating options on the command
line. The configuration file has a 2-level deep tree structure. The first level is
the name of the command (or `:global`) and the second level is the name of the option
which is same as its long variant with `-` replaced by `_`. 

Configuration values are deep-merged in following order (potentially 
overriding): default values, default gem configuration, local configuration, 
command-line options. This tool contains default configurations for some gems 
in the `gem_configurations` directory, which are used if available. An example of
activesupport's configuration file follows:


```yaml
---
:global:
  # default ../jruby/bin/ruby won't work since activesupport is one more dir deeper
  :jruby_truffle_path: '../../jruby/bin/jruby'
  :graal_path: '../../graalvm-jdk1.8.0/bin/java'
:setup:
  :file:
    shims.rb: |
              require 'minitest'
              # mock load_plugins as it loads rubygems
              def Minitest.load_plugins
              end
    bundler.rb: "module Bundler; def self.setup; end; end"
    # mock method_source gem
    method_source.rb: nil
  # do not let bundler to install db gem group
  :without:
    - db
:run:
  :require:
    - shims
```

## Using the tool in CI

Assuming there are stored commands for a given gem like below:
 
```yaml
:stored_commands:
  :ci:
    - :setup
    - :test
  :setup:
    - "git clone git@github.com:lucasocon/openweather.git"
    - "jruby-truffle-tool --dir openweather setup"
  :test: "jruby-truffle-tool --dir openweather run --require-pattern 'test/*_test.rb' -I test -- -e nil"
```

then `jruby-truffle-tool --config openweather stored ci` can be used to run tests of the given gem in CI.

## Example step-by-step

```sh
cd workspace
git clone git@github.com:jruby/jruby.git
cd jruby
tool/jt.rb build # compiles JRuby
cd ..

# assuming rbenv is installed, add your local compiled JRuby to rbenv
ln -s `pwd`/jruby /usr/local/Cellar/rbenv/0.4.0/versions/jruby-local

git clone git@github.com:ruby-concurrency/concurrent-ruby.git
cd concurrent-ruby
git checkout v0.9.1 # latest release
rbenv shell jruby-local # use your compiled JRuby
jruby-truffle-tool setup
jruby-truffle-tool run -S rspec -- spec --format progress # run all tests
# you should see only a few errors
```
