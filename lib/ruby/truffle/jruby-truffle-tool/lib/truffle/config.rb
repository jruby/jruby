require 'pp'
require 'yaml'

include Truffle::Tool::ConfigUtils
include Truffle::Tool::OptionBlocks

stubs = {
    activesupport_isolation: dedent(<<-RUBY),
      require 'active_support/testing/isolation'

      module ActiveSupport
        module Testing
          module Isolation

            def run
              with_info_handler do
                time_it do
                  capture_exceptions do
                    before_setup; setup; after_setup

                    skip 'isolation not supported'
                  end

                  %w{ before_teardown teardown after_teardown }.each do |hook|
                    capture_exceptions do
                      self.send hook
                    end
                  end
                end
              end

              return self # per contract
            end
          end
        end
      end
    RUBY

    bcrypt: dedent(<<-RUBY),
      require 'bcrypt'

      module BCrypt
        class Engine
          def self.hash_secret(secret, salt, _ = nil)
            if valid_secret?(secret)
              if valid_salt?(salt)
                Truffle::Gem::BCrypt.hashpw(secret.to_s, salt.to_s)
              else
                raise Errors::InvalidSalt.new("invalid salt")
              end
            else
              raise Errors::InvalidSecret.new("invalid secret")
            end
          end

          def self.generate_salt(cost = self.cost)
            cost = cost.to_i
            if cost > 0
              if cost < MIN_COST
                cost = MIN_COST
              end
              Truffle::Gem::BCrypt.gensalt(cost)
            else
              raise Errors::InvalidCost.new("cost must be numeric and > 0")
            end
          end
        end
      end
    RUBY

    html_sanitizer: dedent(<<-RUBY),
      require 'action_view'
      require 'action_view/helpers'
      require 'action_view/helpers/sanitize_helper'

      module ActionView
        module Helpers
          module SanitizeHelper
            def sanitize(html, options = {})
              html
            end

            def sanitize_css(style)
              style
            end

            def strip_tags(html)
              html
            end

            def strip_links(html)
              html
            end

            module ClassMethods #:nodoc:
              attr_writer :full_sanitizer, :link_sanitizer, :white_list_sanitizer

              def sanitized_allowed_tags
                []
              end

              def sanitized_allowed_attributes
                []
              end
            end

          end
        end
      end
    RUBY

    kernel_gem: dedent(<<-RUBY),
      module Kernel
        def gem(gem_name, *requirements)
          puts format 'ignoring %s gem activation, already added to $LOAD_PATH by bundler/setup.rb',
                      gem_name
        end
      end
    RUBY

}.reduce({}) do |h, (k, v)|
  file_name = "stub-#{k}"
  h.update k => { setup: { file: { "#{file_name}.rb" => v } },
                  run:   { require: [file_name] } }
end

additions = {
    minitest_reporters: dedent(<<-RUBY)
      require 'rbconfig'
      # add minitest-reporters and its dependencies to $LOAD_PATH
      path = File.expand_path('..', __FILE__)
      %w[ansi-1.5.0 ruby-progressbar-1.8.0 minitest-reporters-1.1.9].each do |gem_dir|
        $:.unshift "\#{path}/../\#{RUBY_ENGINE}/\#{RbConfig::CONFIG['ruby_version']}/gems/\#{gem_dir}/lib"
      end
      # activate
      require "minitest/reporters"
      reporter_class = ENV["CI"] ? Minitest::Reporters::SpecReporter : Minitest::Reporters::ProgressReporter
      Minitest::Reporters.use! reporter_class.new
    RUBY
}.reduce({}) do |h, (k, v)|
  file_name = format '%s.rb', k
  h.update k => { setup: { file: { file_name => v } },
                  run:   { require: [file_name] } }
end

replacements = {
    :bundler => dedent(<<-RUBY),
      module Bundler
        BundlerError = Class.new(Exception)
        def self.setup
        end
      end
    RUBY
    :'bundler/gem_tasks'    => nil,
    :java                   => nil,
    :bcrypt_ext             => nil,
    :method_source          => nil,
    :'rails-html-sanitizer' => nil,
    :nokogiri               => nil
}.reduce({}) do |h, (k, v)|
  h.update k => { setup: { file: { "#{k}.rb" => v || %[puts "loaded '#{k}.rb' an empty replacement"] } } }
end

# add required replacements to stubs
deep_merge!(stubs.fetch(:bcrypt),
            replacements.fetch(:java),
            replacements.fetch(:bcrypt_ext))
deep_merge!(stubs.fetch(:html_sanitizer),
            replacements.fetch(:'rails-html-sanitizer'),
            replacements.fetch(:nokogiri))

def exclusion_file(gem_name)
  data = YAML.load_file(__dir__ + "/#{gem_name}_exclusions.yaml")
  data.pretty_inspect
end

def exclusions_for(name, ignore_missing: false)
  { setup: { file: { 'excluded-tests.rb' => format(dedent(<<-RUBY), exclusion_file(name)) } } }
    failures = %s
    require 'truffle/exclude_rspec_examples'
    Truffle::Tool.exclude_rspec_examples failures, ignore_missing: #{!!ignore_missing}
  RUBY
end

rails_common =
    deep_merge replacements.fetch(:bundler),
               stubs.fetch(:kernel_gem),
               additions.fetch(:minitest_reporters),
               setup: { without: %w(db job) },
               run:   { environment: { 'N' => 1 },
                        require:     %w(rubygems date bigdecimal pathname openssl-stubs) }

Truffle::Tool.add_config :activesupport,
                         deep_merge(
                             rails_common,
                             stubs.fetch(:activesupport_isolation),
                             replacements.fetch(:method_source),
                             exclusions_for(:activesupport))

Truffle::Tool.add_config :activemodel,
                         deep_merge(
                             rails_common,
                             stubs.fetch(:activesupport_isolation),
                             stubs.fetch(:bcrypt))

Truffle::Tool.add_config :actionpack,
                         deep_merge(
                             rails_common,
                             stubs.fetch(:html_sanitizer),
                             exclusions_for(:actionpack))

Truffle::Tool.add_config :railties,
                         deep_merge(rails_common,
                                    stubs.fetch(:activesupport_isolation),
                                    exclusions_for(:railties),
                                    run: { require: %w[bundler.rb] })

Truffle::Tool.add_config :'concurrent-ruby',
                         setup: { file: { "stub-processor_number.rb" => dedent(<<-RUBY) } },
                              # stub methods calling #system
                              require 'concurrent'
                              module Concurrent
                                module Utility
                                  class ProcessorCounter
                                    def compute_processor_count
                                      2
                                    end
                                    def compute_physical_processor_count
                                      2
                                    end
                                  end
                                end
                              end
                         RUBY
                         run: { require: %w(stub-processor_number) }

Truffle::Tool.add_config :monkey_patch,
                         replacements.fetch(:bundler)

Truffle::Tool.add_config :openweather,
                         replacements.fetch(:'bundler/gem_tasks')

Truffle::Tool.add_config :psd,
                         replacements.fetch(:nokogiri)

Truffle::Tool.add_config :actionview,
                         deep_merge(rails_common,
                                    exclusions_for(:actionview, ignore_missing: true),
                                    stubs.fetch(:html_sanitizer))

class Truffle::Tool::CIEnvironment
  def rails_ci(has_exclusions: false, skip_test_files: [], require_pattern: 'test/**/*_test.rb')
    rails_ci_setup has_exclusions: has_exclusions
    set_result rails_ci_run has_exclusions:  has_exclusions,
                            skip_test_files: skip_test_files,
                            require_pattern: require_pattern
  end

  def rails_ci_setup(has_exclusions: false)
    options           = {}
    options[:debug]   = ['-d', '--[no-]debug', 'Run tests with remote debugging enabled.', STORE_NEW_VALUE, false]
    options[:exclude] = ['--[no-]exclusion', 'Exclude known failing tests', STORE_NEW_VALUE, true] if has_exclusions

    declare_options options
    repository_name 'rails'

    use_only_https_git_paths!

    has_to_succeed setup
  end

  def rails_ci_run(has_exclusions: false, skip_test_files: [], require_pattern: 'test/**/*_test.rb')
    run([*(['--exclude-pattern', *skip_test_files.join('|')] unless skip_test_files.empty?),
         '--require-pattern', require_pattern,
         *(%w[-r excluded-tests] if has_exclusions && option(:exclude)),
         *(%w[--debug] if option(:debug)),
         *%w[-- -I test -e nil]])
  end
end

Truffle::Tool.add_ci_definition :actionpack do
  subdir 'actionpack'
  rails_ci has_exclusions: true
end

Truffle::Tool.add_ci_definition :activemodel do
  subdir 'activemodel'
  rails_ci
end

Truffle::Tool.add_ci_definition :activesupport do
  subdir 'activesupport'
  rails_ci has_exclusions: true
end

Truffle::Tool.add_ci_definition :railties do
  subdir 'railties'
  rails_ci has_exclusions:  true,
           skip_test_files: %w[
              test/application/asset_debugging_test.rb
              test/application/assets_test.rb
              test/application/bin_setup_test.rb
              test/application/configuration_test.rb
              test/application/console_test.rb
              test/application/generators_test.rb
              test/application/loading_test.rb
              test/application/mailer_previews_test.rb
              test/application/middleware_test.rb
              test/application/multiple_applications_test.rb
              test/application/paths_test.rb
              test/application/rackup_test.rb
              test/application/rake_test.rb
              test/application/rendering_test.rb
              test/application/routing_test.rb
              test/application/runner_test.rb
              test/application/test_runner_test.rb
              test/application/test_test.rb
              test/application/url_generation_test.rb
              test/application/configuration/base_test.rb
              test/application/configuration/custom_test.rb
              test/application/initializers/frameworks_test.rb
              test/application/initializers/hooks_test.rb
              test/application/initializers/i18n_test.rb
              test/application/initializers/load_path_test.rb
              test/application/initializers/notifications_test.rb
              test/application/middleware/cache_test.rb
              test/application/middleware/cookies_test.rb
              test/application/middleware/exceptions_test.rb
              test/application/middleware/remote_ip_test.rb
              test/application/middleware/sendfile_test.rb
              test/application/middleware/session_test.rb
              test/application/middleware/static_test.rb
              test/application/rack/logger_test.rb
              test/application/rake/dbs_test.rb
              test/application/rake/migrations_test.rb
              test/application/rake/notes_test.rb
              test/railties/engine_test.rb
              test/railties/generators_test.rb
              test/railties/mounted_engine_test.rb
              test/railties/railtie_test.rb
              test/fixtures
              test/rails_info_controller_test
              test/commands/console_test]
end

Truffle::Tool.add_ci_definition :algebrick do

  has_to_succeed setup

  set_result run(%w[test/algebrick_test.rb])
end

Truffle::Tool.add_ci_definition :actionview do
  subdir 'actionview'
  rails_ci_setup(has_exclusions: true)
  results = [
      rails_ci_run(has_exclusions:  true,
                   require_pattern: 'test/template/**/*_test.rb'),
      rails_ci_run(has_exclusions:  true,
                   require_pattern: 'test/actionpack/**/*_test.rb')
  # TODO (pitr-ch 17-Nov-2016): requires ActiveRecord connection to database to run, uses sqlite
  # rails_ci_run(has_exclusions:  true,
  #              require_pattern: 'test/activerecord/*_test.rb')
  ]

  set_result results.all?
end
