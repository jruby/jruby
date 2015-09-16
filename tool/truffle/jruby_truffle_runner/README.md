# JRuby+Truffle Runner

`jruby+truffle_runner` gem is a small command line utility designed to run and
test Ruby gems and applications on JRuby+Truffle Ruby runtime. It uses other
Ruby implementation to prepare environment and to execute files, tests
on JRuby+Truffle. It is a temporary tool to make working with JRuby+Truffle
easy until it fully supports `rubygems` and `bundler`

## Installation

It assumes that you have another Ruby available, if you have JRuby's git
repository clone it can be compiled and used to run this tool. Rbenv is quite
good tool to install and manage different Rubies. To add your compiled JRuby
just add symlink from rbenv's `versions` directory pointing to JRuby's local
repository,
e.g. `ln -s ~/labs/jruby /usr/local/Cellar/rbenv/0.4.0/versions/jruby-local`.

There are 3 options.

1.  Install the gem from rubygems.org (Not yet published).
    Install the gem `gem install jruby+truffle_runner`
2.  Use `jt` tool from JRuby's repository.
    Run `jt install-tool` to install the gem from the cloned repository.
3.  Create alias for the gem's executable. E.g. add
    `alias jruby+truffle="~/path-to-jruby/tool/truffle/jruby_truffle_runner/bin/jruby+truffle"`
    to your `.bashrc`.

Then run `jruby+truffle --help` to see available subcommands.

## Setup

There is a `setup` subcommand to create environment for JRuby+Truffle.

-   Go to directory of a gem/application you would like to test.
-   Run `jruby+truffle setup`

It copies default configuration (part of the gem) if one is available for a
given gem (it looks for a `gemspec` in current directory). It installs all
required gems (based on `Gemfile`) to local bundle (default path:
`.jruby+truffle_bundle`), and executes other steps defined in the configuration
file (`.jruby+truffle.yaml`) or as command line options (see `jruby+truffle
setup --help` to learn what additional setup steps are available, or see one of
the bundled configuration files found in `gem_configurations` directory). After
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

## Local configuration

Options which are required always or are part of the setup step can
pre-configured in `.jruby+truffle.yaml` file to avoid repeating on command
line. The configuration file has a 2 level deep tree structure. First level is
a name of the command (or `:global`), second level is the name of the option
which is same as its long variant with `-` replaced by `_`. Its values are
deep-merged with default values, then command-line options are applied. This
tool contains default configurations for some gems in `gem_configurations`
directory, they are copied when there is no configuration present. As example
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
truffle setup
truffle run -S rspec -- spec --format progress # run all tests
# you should only see few errors
```
