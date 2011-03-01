require 'uri'
require 'rubygems/spec_fetcher'
require 'rubygems/remote_fetcher'

module Gem
  module MavenUtils
    def maven_name?(name)
      name = name.source if Regexp === name
      name =~ /^[^:.\/!?#%]{2,}[.:][^:.\/!?#%]{2,}/
    end

    def maven_source_uri?(source_uri)
      source_uri.scheme == "mvn" || source_uri.host == "maven"
    end

    def maven_sources
      Gem.sources.select {|x| x =~ /^mvn:/}
    end

    def maven_spec?(gemname, source_uri)
      maven_name?(gemname) && maven_source_uri?(source_uri)
    end
  end

  class Maven3NotFound < StandardError; end

  class RemoteFetcher
    include MavenUtils

    def download_maven(spec, local_gem_path)
      FileUtils.cp Gem::Maven::Gemify.new(maven_sources).generate_gem(spec.name, spec.version), local_gem_path
      local_gem_path
    end
    private :download_maven
  end

  class SpecFetcher
    include MavenUtils

    alias orig_find_matching_with_errors find_matching_with_errors
    def find_matching_with_errors(dependency, all = false, matching_platform = true, prerelease = false)
      if maven_name? dependency.name
        begin
          result = maven_find_matching_with_errors(dependency)
        rescue Gem::Maven3NotFound => e
          raise e
        rescue => e
          warn "maven find dependency failed for #{dependency}: #{e.to_s}" if Gem::Maven::Gemify.verbose?
        end
      end
      if result && !result.flatten.empty?
        result
      else
        orig_find_matching_with_errors(dependency, all, matching_platform, prerelease)
      end
    end

    alias orig_list list
    def list(*args)
      sources = Gem.sources
      begin
        Gem.sources -= maven_sources
        return orig_list(*args)
      ensure
        Gem.sources = sources
      end
    end

    alias orig_load_specs load_specs
    def load_specs(source_uri, file)
      return if source_uri.scheme == "mvn"
      orig_load_specs(source_uri, file)
    end

    private
    def maven_generate_spec(spec)
      specfile = Gem::Maven::Gemify.new(maven_sources).generate_spec(spec[0], spec[1])
      return nil unless specfile
      Marshal.dump(Gem::Specification.from_yaml(File.read(specfile)))
    end

    # use maven to locate (generate) the specification for the dependency in question
    def maven_find_matching_with_errors(dependency)
      specs_and_sources = []
      if dependency.name.is_a? Regexp
        dep_name = dependency.name.source.sub(/\^/, '')
      else
        dep_name = dependency.name
      end

      Gem::Maven::Gemify.new(maven_sources).get_versions(dep_name).each do |version|
        # maven-versions which start with an letter get "0.0.0." prepended to
        # satisfy gem-version requirements
        if dependency.requirement.satisfied_by? Gem::Version.new "#{version.sub(/^0.0.0./, '1.')}"
          specs_and_sources.push [[dep_name, version, "java"], "http://maven/"]
        end
      end

      [specs_and_sources, []]
    end
  end

  module Maven
    class Gemify
      BASE_GOAL = "de.saumya.mojo:gemify-maven-plugin:0.22.0"
      MAVEN_REPOS = { "central" => nil } # gets updated at maven load time

      attr_reader :repositories

      def initialize(*repositories)
        maven                   # ensure maven initialized
        @repositories = repositories.length > 0 ? [repositories].flatten : [MAVEN_REPOS["central"]]
        @repositories.map! do |r|
          u = URI === r ? r : URI.parse(r)
          if u.scheme == "mvn"
            if u.opaque == "central"
              u = URI.parse(MAVEN_REPOS["central"])
            else
              u.scheme = "http"
            end
          end
          u
        end
      end

      @@verbose = false
      def self.verbose?
        @@verbose || $DEBUG
      end
      def verbose?
        self.class.verbose?
      end
      def self.verbose=(v)
        @@verbose = v
      end

      private
      def new_repository(uri)
        snapshots = ArtifactRepositoryPolicy.new(false,
                                                 ArtifactRepositoryPolicy::UPDATE_POLICY_NEVER,
                                                 ArtifactRepositoryPolicy::CHECKSUM_POLICY_FAIL)
        releases = ArtifactRepositoryPolicy.new(true,
                                                ENV['MVN_UPDATE_POLICY'] || ArtifactRepositoryPolicy::UPDATE_POLICY_NEVER,
                                                ENV['MVN_CHECKSUM_POLICY'] || ArtifactRepositoryPolicy::CHECKSUM_POLICY_FAIL)
        MavenArtifactRepository.new(uri.host, uri.to_s, DefaultRepositoryLayout.new,
                                    snapshots, releases)
      end

      def self.java_imports
        %w(
           org.codehaus.plexus.classworlds.ClassWorld
           org.codehaus.plexus.DefaultContainerConfiguration
           org.codehaus.plexus.DefaultPlexusContainer
           org.apache.maven.Maven
           org.apache.maven.repository.RepositorySystem
           org.apache.maven.execution.DefaultMavenExecutionRequest
           org.apache.maven.artifact.repository.MavenArtifactRepository
           org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout
           org.apache.maven.artifact.repository.ArtifactRepositoryPolicy
          ).each {|i| java_import i }
      end

      def self.create_maven
        require 'java' # done lazily, so we're not loading it all the time
        bin = nil
        if ENV['M2_HOME'] # use M2_HOME if set
          bin = File.join(ENV['M2_HOME'], "bin")
        else
          ENV['PATH'].split(File::PATH_SEPARATOR).detect do |path|
            mvn = File.join(path, "mvn")
            if File.exists?(mvn)
              if File.symlink?(mvn)
                link = File.readlink(mvn)
                if link =~ /^\// # is absolute path
                  bin = File.dirname(File.expand_path(link))
                else # is relative path so join with dir of the maven command
                  bin = File.dirname(File.expand_path(File.join(File.dirname(mvn), link)))
                end
              else # is no link so just expand it
                bin = File.expand_path(path)
              end
            else
              nil
            end
          end
        end
        bin = "/usr/share/maven2/bin" if bin.nil? # OK let's try debian default
        if File.exists?(bin)
          @mvn = File.join(bin, "mvn")
          if Dir.glob(File.join(bin, "..", "lib", "maven-core-3.*jar")).size == 0
            begin
              gem 'ruby-maven', ">=0"
              bin = File.dirname(Gem.bin_path('ruby-maven', "rmvn"))
              @mvn = File.join(bin, "rmvn")
            rescue LoadError
              bin = nil
            end
          end
        else
          bin = nil
        end
        raise Gem::Maven3NotFound.new("can not find maven3 installation. install ruby-maven with\n\n\tjruby -S gem install ruby-maven --pre\n\n") if bin.nil?

        warn "Using Maven install at #{bin}" if verbose?

        boot = File.join(bin, "..", "boot")
        lib = File.join(bin, "..", "lib")
        ext = File.join(bin, "..", "ext")
        (Dir.glob(lib + "/*jar")  + Dir.glob(boot + "/*jar")).each {|path| require path }

        java.lang.System.setProperty("classworlds.conf", File.join(bin, "m2.conf"))
        java.lang.System.setProperty("maven.home", File.join(bin, ".."))
        java_imports

        class_world = ClassWorld.new("plexus.core", java.lang.Thread.currentThread().getContextClassLoader());
        config = DefaultContainerConfiguration.new
        config.set_class_world class_world
        config.set_name "ruby-tools"
        MAVEN_REPOS["central"] = RepositorySystem::DEFAULT_REMOTE_REPO_URL
        MAVEN_REPOS["local"] = RepositorySystem::defaultUserLocalRepository.toURI.toString
        container = DefaultPlexusContainer.new(config);
        container.lookup(Maven.java_class)
      end

      def self.maven
        @maven ||= create_maven
      end
      def maven; self.class.maven; end

      def self.temp_dir
        @temp_dir ||=
          begin
            f = java.io.File.createTempFile("gemify", "")
            f.delete
            f.mkdir
            f.deleteOnExit
            f.absolute_path
          end
      end
      def temp_dir; self.class.temp_dir; end

      def execute(goal, gemname, version, props = {})
        request = DefaultMavenExecutionRequest.new
        request.set_show_errors verbose?
        request.user_properties.put("gemify.tempDir", temp_dir)
        request.user_properties.put("gemify.gemname", gemname)
        request.user_properties.put("gemify.version", version.to_s) if version
        props.each do |k,v|
          request.user_properties.put(k.to_s, v.to_s)
        end
        request.set_goals [goal]
        request.set_logging_level 0
        request.set_remote_repositories @repositories.map {|r| new_repository(r) }

        out = java.lang.System.out
        string_io = java.io.ByteArrayOutputStream.new
        java.lang.System.setOut(java.io.PrintStream.new(string_io))
        result = maven.execute request
        java.lang.System.out = out
        if request.is_show_errors
          puts "maven goals:"
          request.goals.each { |g| puts "\t#{g}" }
          puts "system properties:"
          request.getUserProperties.map.each { |k,v| puts "\t#{k} => #{v}" }

          result.exceptions.each do |e|
            e.print_stack_trace
            string_io.write(e.get_message.to_java_string.get_bytes)
          end
        end
        string_io.to_s
      end

      public
      def get_versions(gemname)
        name = maven_name(gemname)
        result = execute("#{BASE_GOAL}:versions", name, nil)

        if result =~ /#{name} \[/
          result = result.gsub(/\r?\n/, '').sub(/.*\[/, "").sub(/\]/, '').gsub(/ /, '').split(',')
          puts "versions: #{result.inspect}" if verbose?
          result
        else
          []
        end
      end

      def generate_spec(gemname, version)
        result = execute("#{BASE_GOAL}:gemify", maven_name(gemname), version, "gemify.onlySpecs" => true)
        path = result.gsub(/\r?\n/, '')
        if path =~ /gemspec: /
          path = path.sub(/.*gemspec: /, '')
          if path.size > 0
            result = File.expand_path(path)
            java.io.File.new(result).deleteOnExit
            result
          end
        end
      end

      def generate_gem(gemname, version)
        result = execute("#{BASE_GOAL}:gemify", maven_name(gemname), version)
        path = result.gsub(/\r?\n/, '')
        if path =~ /gem: /

          path = path.sub(/.*gem: /, '')
          if path.size > 0
            result = File.expand_path(path)
            java.io.File.new(result).deleteOnExit
            result
          end
        else
          warn result.sub(/.*Missing Artifacts:\s+/, '').gsub(/\tmvn/, "\t#{@mvn}")
          raise "error gemify #{gemname}:#{version}"
        end
      end

      def maven_name(gemname)
        gemname = gemname.source if Regexp === gemname
        gemname.gsub(/:/, '.')
      end
    end
  end
end
