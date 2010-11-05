if defined?(JRUBY_VERSION)
  
  class Gem::Specification
    # return whether the spec name represents a maven artifact
    def self.maven_name?(name)
      name =~ /\./
    end
  end
  
  class Gem::RemoteFetcher
    def download_maven(spec, local_gem_path)
      require 'rubygems/maven_gemify'
      FileUtils.cp Gem::Maven::Gemify.generate_gem(spec.name, spec.version), local_gem_path
      local_gem_path
    end
    private :download_maven
  end
  
  class Gem::SpecFetcher
    def gemify_generate_spec(spec)
      require 'rubygems/maven_gemify'
      specfile = Gem::Maven::Gemify.generate_spec(spec[0], spec[1])
      Marshal.dump(Gem::Specification.from_yaml(File.read(specfile)))
    end
    private :gemify_generate_spec
  
    # use maven to locate (generate) the specification for the dependency in question
    def find_matching_using_maven(dependency)
      specs_and_sources = []
      
      require 'rubygems/maven_gemify'
      Gem::Maven::Gemify.get_versions(dependency.name).each do |version|
        if dependency =~ Gem::Dependency.new(dependency.name, version)
          specs_and_sources.push [[dependency.name, version, "java"], "http://maven/"]
        end
      end
      
      [specs_and_sources, []]
    end
    private :find_matching_using_maven
  end
  
  module Gem::Maven
    class Gemify
      BASE_GOAL = "de.saumya.mojo:gemify-maven-plugin:0.22.0"
      
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
          if Dir.glob(File.join(bin, "..", "lib", "maven-core-3.*jar")).size == 0
            begin
              gem 'ruby-maven', ">=0"
              bin = File.dirname(Gem.bin_path('ruby-maven', "rmvn"))
            rescue LoadError
              bin = nil
            end
          end
        else
          bin = nil
        end
        raise "can not find maven3 installation. install ruby-maven with\n\n\tjruby -S gem install ruby-maven --pre\n\n" if bin.nil?
        warn "Installing from Maven using install at #{bin}"
        boot = File.join(bin, "..", "boot")
        lib = File.join(bin, "..", "lib")
        ext = File.join(bin, "..", "ext")
        classpath = (Dir.glob(lib + "/*jar")  + Dir.glob(boot + "/*jar"))
        
        java.lang.System.setProperty("classworlds.conf", File.join(bin, "m2.conf"))
        
        java.lang.System.setProperty("maven.home", File.join(bin, ".."))
        classpath.each do |path|
          require path
        end
        
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
        r.set_show_errors true
        r.user_properties.put("gemify.skipDependencies", "true")
        r.user_properties.put("gemify.tempDir", temp_dir)
        r.user_properties.put("gemify.gemname", gemname)
        r.user_properties.put("gemify.version", version.to_s) if version
        props.each do |k,v|
          r.user_properties.put(k.to_s, v.to_s)
        end
        r.set_goals [goal]
        r.set_logging_level 0
        
        #p r.goals.to_s
        #p r.getUserProperties.map
        out = java.lang.System.out
        string_io = java.io.ByteArrayOutputStream.new
        java.lang.System.setOut(java.io.PrintStream.new(string_io))
        result = maven.execute( r );
        java.lang.System.out = out
        result.exceptions.each { |e| e.print_stack_trace }
        string_io.to_s
      end
      
      public
      
      def self.get_versions(gemname)
        #        p "versions"
        #        p gemname
        result = execute("#{BASE_GOAL}:versions", gemname, nil)
        
        result.gsub(/\n/, '').sub(/.*\[/, "").sub(/\]/, '').gsub(/ /, '').split(',')
      end
      
      def self.generate_spec(gemname, version)        
        #     puts "generate spec"
        #     p gemname
        #     p version
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
        #    p "generate gem"
        #    p gemname
        #    p version.to_s
        
        result = execute("#{BASE_GOAL}:gemify", gemname, version)
        path = result.gsub(/\n/, '')
        if path =~ /gem: /
          
          path = path.sub(/.*gem: /, '')
          if path.size > 0
            result = File.expand_path(path)
            java.io.File.new(result).deleteOnExit
            result
          end
        end
      end
    end
  end
end
