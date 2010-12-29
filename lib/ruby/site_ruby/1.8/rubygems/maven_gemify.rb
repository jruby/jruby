module Gem
  class Specification
    # return whether the spec name represents a maven artifact
    def self.maven_name?(name)
      case name
      when Regexp
        name.source =~ /\./
      else
        name =~ /\./
      end
    end
  end

  class RemoteFetcher
    def download_maven(spec, local_gem_path)
      FileUtils.cp Gem::Maven::Gemify.generate_gem(spec.name, spec.version), local_gem_path
      local_gem_path
    end
    private :download_maven
  end

  class SpecFetcher
    def gemify_generate_spec(spec)
      specfile = Gem::Maven::Gemify.generate_spec(spec[0], spec[1])
      Marshal.dump(Gem::Specification.from_yaml(File.read(specfile)))
    end
    private :gemify_generate_spec

    # use maven to locate (generate) the specification for the dependency in question
    def find_matching_using_maven(dependency)
      specs_and_sources = []
      if dependency.name.is_a? Regexp
        dep_name = dependency.name.source.sub(/\^/, '')
      else
        dep_name = dependency.name
      end

      Gem::Maven::Gemify.get_versions(dep_name).each do |version|
        # maven-versions which start with an letter get "0.0.0." prepended to
        # satisfy gem-version requirements
        if dependency.requirement.satisfied_by? Gem::Version.new "#{version.sub(/^0.0.0./, '1.')}"
          specs_and_sources.push [[dep_name, version, "java"], "http://maven/"]
        end
      end

      [specs_and_sources, []]
    end
    private :find_matching_using_maven
  end

  module Maven
    class Gemify
      BASE_GOAL = "de.saumya.mojo:gemify-maven-plugin:0.22.0"

      @@verbose = false
      def self.verbose?
        @@verbose
      end
      def self.verbose=(v)
        @@verbose = v
      end

      private

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
        raise "can not find maven3 installation. install ruby-maven with\n\n\tjruby -S gem install ruby-maven --pre\n\n" if bin.nil?

        warn "Using Maven install at #{bin}" if verbose?

        boot = File.join(bin, "..", "boot")
        lib = File.join(bin, "..", "lib")
        ext = File.join(bin, "..", "ext")
        (Dir.glob(lib + "/*jar")  + Dir.glob(boot + "/*jar")).each {|path| require path }

        java.lang.System.setProperty("classworlds.conf", File.join(bin, "m2.conf"))
        java.lang.System.setProperty("maven.home", File.join(bin, ".."))
        import "org.codehaus.plexus.classworlds"
        java_import "org.codehaus.plexus.DefaultContainerConfiguration"
        java_import "org.codehaus.plexus.DefaultPlexusContainer"
        java_import "org.apache.maven.Maven"
        java_import "org.apache.maven.execution.DefaultMavenExecutionRequest"

        class_world = ClassWorld.new("plexus.core", java.lang.Thread.currentThread().getContextClassLoader());
        config = DefaultContainerConfiguration.new
        config.set_class_world class_world
        config.set_name "ruby-tools"
        container = DefaultPlexusContainer.new(config);
        container.lookup(Maven.java_class)
      end

      def self.maven_get
        @maven ||= create_maven
      end

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

      def self.execute(goal, gemname, version, props = {})
        maven = maven_get
        r = DefaultMavenExecutionRequest.new
        r.set_show_errors verbose?
        r.user_properties.put("gemify.skipDependencies", "true")
        r.user_properties.put("gemify.tempDir", temp_dir)
        r.user_properties.put("gemify.gemname", gemname)
        r.user_properties.put("gemify.version", version.to_s) if version
        props.each do |k,v|
          r.user_properties.put(k.to_s, v.to_s)
        end
        r.set_goals [goal]
        r.set_logging_level 0

        out = java.lang.System.out
        string_io = java.io.ByteArrayOutputStream.new
        java.lang.System.setOut(java.io.PrintStream.new(string_io))
        result = maven.execute( r );
        java.lang.System.out = out
        if r.is_show_errors
          puts "maven goals:"
          r.goals.each { |g| puts "\t#{g}" }
          puts "system properties:"
          r.getUserProperties.map.each { |k,v| puts "\t#{k} => #{v}" }

          result.exceptions.each do |e|
            e.print_stack_trace
            string_io.write(e.get_message.to_java_string.get_bytes)
          end
        end
        string_io.to_s
      end

      public

      def self.get_versions(gemname)
        gemname = gemname.source.sub(/\^/, '') if gemname.is_a? Regexp
        result = execute("#{BASE_GOAL}:versions", gemname, nil)

        if result =~ /\[/ && result =~ /\]/
          result = result.gsub(/\n/, '').sub(/.*\[/, "").sub(/\]/, '').gsub(/ /, '').split(',')
          puts "versions: #{result.inspect}" if verbose?
          result
        else
          []
        end
      end

      def self.generate_spec(gemname, version)
        result = execute("#{BASE_GOAL}:gemify", gemname, version, {"gemify.onlySpecs" => true })
        path = result.gsub(/\n/, '')
        if path =~ /gemspec: /
          path = path.sub(/.*gemspec: /, '')
          if path.size > 0
            result = File.expand_path(path)
            java.io.File.new(result).deleteOnExit
            result
          end
        end
      end

      def self.generate_gem(gemname, version)
        result = execute("#{BASE_GOAL}:gemify", gemname, version)
        path = result.gsub(/\n/, '')
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
    end
  end
end
