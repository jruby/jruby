# JRuby+Truffle Runner

`jruby+truffle` is a small command line utility designed to run and
test Ruby gems and applications on JRuby+Truffle Ruby runtime. It uses other
Ruby implementation to prepare environment and to execute files, tests
on JRuby+Truffle. It is a temporary tool to make working with JRuby+Truffle
easy until it fully supports `rubygems` and `bundler`

## Installation

The command line tool is part of the JRuby distribution. It's available in
JRuby's bin directory.

Just run `jruby+truffle --help` to see available subcommands.

## Setup

There is a `setup` subcommand to create environment for JRuby+Truffle.

-   Go to directory of a gem/application you would like to test.
-   Run `jruby+truffle setup`

It uses default configuration (part of the tool) if one is available for a
given gem (it looks for a `gemspec` in current directory). It installs all
required gems (based on `Gemfile`) to local bundle (default path:
`.jruby+truffle_bundle`), and executes other steps defined in the configuration
files or as command line options (see `jruby+truffle
setup --help` to learn what additional setup steps are available, or see one of
the default configuration files found in `gem_configurations` directory). After
it finishes, the `run` subcommand can be used.

## Running

After the environment is set the gem can be used to execute code, files, or
gem's executables on JRuby+Truffle in prepared environment. Examples follows
(options after `--` are interpreted by Ruby, options before `--` are options
for this tool):

-   `jruby+truffle run -- file.rb` - executes file.rb
-   `jruby+truffle run -- -e '1+1'` - evaluates 1+1 expression
-   `jruby+truffle run -- -I test test/a_test_file_test.rb` - runs a test-file
-   `jruby+truffle run -S rspec -- spec/a_spec_file_spec.rb` - runs a spec file
    using executable of rspec gem
-   `jruby+truffle run --require mocks -- file.rb` - executes file.rb, but
    requires mocks first. (mocks can be made to load always by putting the
    option to configuration file (`.jruby+truffle.yaml`) instead)

See `jruby+truffle run --help` to see all available options.

## Clean up

To remove all files added during setup phase run `jruby+truffle clean`, it'll
only keep `.jruby+truffle.yaml` configuration file for future re-setup.

## Pre-configuration

Options which are required always or are part of the setup step can
pre-configured in the default configuration (included in the tool) or in local 
`.jruby+truffle.yaml` configuration file to avoid repeating options on command
line. The configuration file has a 2 level deep tree structure. First level is
a name of the command (or `:global`), second level is the name of the option
which is same as its long variant with `-` replaced by `_`. 

Configuration values are deep-merged in following order (potentially 
overriding): default values, default gem configuration, local configuration, 
command-line options. This tool contains default configurations for some gems 
in `gem_configurations` directory, they are used if available. An example of
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

Assuming there are similar stored commands for a given gem:
 
```yaml
:stored_commands:
  :ci:
    - :setup
    - :test
  :setup:
    - "git clone git@github.com:lucasocon/openweather.git"
    - "jruby+truffle --dir openweather setup"
  :test: "jruby+truffle --dir openweather run --require-pattern 'test/*_test.rb' -I test -- -e nil"
```

then `jruby+truffle --config openweather stored ci` can be used to run tests of the given gem in CI.

## Example step-by-step

```sh
cd workspace
git clone git@github.com:jruby/jruby.git
cd jruby
tool/jt.rb build # compile JRuby
cd ..

# assuming rbenv is installed, add your local compiled JRuby to rbenv
ln -s ~/labs/jruby /usr/local/Cellar/rbenv/0.4.0/versions/jruby-local

git clone git@github.com:ruby-concurrency/concurrent-ruby.git
cd concurrent-ruby
git checkout v0.9.1 # latest release
rbenv shell jruby-local # use your compiled JRuby
jruby+truffle setup
jruby+truffle run -S rspec -- spec --format progress # run all tests
# you should see only few errors
```
