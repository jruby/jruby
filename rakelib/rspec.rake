# Definitions:
#  MSPEC_FILE:
#    tagged       - runs specs which a MSPEC_FILE
#    all          - runs specs without specifying an MSPEC_FILE
#    !all,!tagged - runs specs with an MSPEC_FILE
#  RUNTIME:
#    interpreted  - -X-C (or OFF)
#    compiled     - JIT w/ threshold 0
#    precompiled  - AOT (or FORCE) 
#  RUBYSPEC:
#    latest       - grab head of rubyspecs
#    stable       - get saved blessed version (see RUBYSPECS_VERSION)

# FIXME: Make Rubyspecs FAILED message appear?

namespace :spec do
  # Put Rake on the load path for JI specs without requiring rubygems
  rake_location = File.join(Gem.loaded_specs['rake'].full_gem_path, "lib")
  gem 'rspec'
  require 'rspec/core/rake_task'

  {"" => "--1.8", "19" => "--1.9"}.each do |version_suffix, version_arg|
    permute_specs "ji#{version_suffix}", COMPILE_FLAGS, "test:compile" do |t|
      t.ruby_opts << " -I#{rake_location}"
      t.ruby_opts << version_arg
      t.rspec_opts ||= []
      t.rspec_opts << "--options" << "spec/java_integration/spec.opts"
      t.pattern = 'spec/java_integration/**/*_spec.rb'
    end

    permute_specs "ji#{version_suffix}:quiet", COMPILE_FLAGS, "test:compile" do |t|
      t.ruby_opts = " -I#{rake_location}"
      t.ruby_opts = version_arg
      t.rspec_opts ||= []
      t.rspec_opts << "--options" << "spec/java_integration/spec.quiet.opts"
      t.pattern = 'spec/java_integration/**/*_spec.rb'
    end

    permute_specs "compiler#{version_suffix}", COMPILE_FLAGS do |t|
      t.ruby_opts = version_arg
      t.pattern = 'spec/compiler/**/*_spec.rb'
    end

    permute_specs "profiler#{version_suffix}", COMPILE_FLAGS do |t|
      t.ruby_opts = "--profile #{version_arg}"
      t.pattern = 'spec/profiler/**/*_spec.rb'
    end

    permute_specs "ffi#{version_suffix}", COMPILE_FLAGS do |t|
      t.ruby_opts = version_arg
      t.pattern = 'spec/ffi/**/*_spec.rb'
    end

    permute_specs "java_signature_parser#{version_suffix}", COMPILE_FLAGS do |t|
      t.ruby_opts = version_arg
      t.rspec_opts ||= []
      #    t.spec_opts << "--options" << "spec/java_integration/spec.quiet.opts"
      t.pattern = 'spec/grammar/**/*_spec.rb'
    end

    permute_specs "regression#{version_suffix}", COMPILE_FLAGS do |t|
      t.ruby_opts = version_arg
      t.pattern = 'spec/regression/**/*_spec.rb'
    end
  end
end
