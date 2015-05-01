class ImportedGem
  attr_reader :name, :default_gem, :pom_version_key, :ruby_version

  def initialize( name, pom_version_key, default_gem, ruby_version = nil )
    @name = name
    @default_gem = default_gem
    @pom_version_key = pom_version_key
    @ruby_version = ruby_version
  end

  def version
    if pom_version_key =~ /.version/
      "${#{pom_version_key}}"
    else
      pom_version_key
    end
  end
end

# the versions are declared in ../pom.xml
default_gems =
  [
   ImportedGem.new( 'jruby-openssl', '0.9.7', true ),
   ImportedGem.new( 'jruby-readline', '1.0', false ),
   ImportedGem.new( 'rake', 'rake.version', true ),
   ImportedGem.new( 'rdoc', 'rdoc.version', true ),
   ImportedGem.new( 'minitest', 'minitest.version', true ),
   ImportedGem.new( 'test-unit', 'test-unit.version', true ),
   ImportedGem.new( 'power_assert', 'power_assert.version', true ),
   ImportedGem.new( 'psych', '2.0.14.pre1', true ),
   ImportedGem.new( 'json', 'json.version', true ),
   ImportedGem.new( 'jar-dependencies', '0.1.13', true )
  ]

project 'JRuby Lib Setup' do
 
  # TODO move those to method to ruby-maven
  class ::Java::JavaIo::File
    def to_pathname
      to_s.gsub( /\\/, '/' )
    end
  end
  class ::Java::JavaLang::String
    def to_pathname
      to_s.gsub( /\\/, '/' )
    end
  end
  class ::String
    def to_pathname
      self.gsub( /\\/, '/' )
    end
  end

  version = File.read( File.join( basedir, '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id 'jruby-stdlib'
  inherit "org.jruby:jruby-parent", version

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'tesla.version' => '0.1.1',
              'jruby.plugins.version' => '1.0.9',
              'gem.home' => '${basedir}/ruby/gems/shared',
              # we copy everything into the target/classes/META-INF
              # so the jar plugin just packs it - see build/resources below
              'jruby.complete.home' => '${project.build.outputDirectory}/META-INF/jruby.home',
              'jruby.complete.gems' => '${jruby.complete.home}/lib/ruby/gems/shared' )

  # just depends on jruby-core so we are sure the jruby.jar is in place
  jar "org.jruby:jruby-core:#{version}", :scope => 'test'

  repository( :url => 'http://rubygems-proxy.torquebox.org/releases',
              :id => 'rubygems-releases' )

  plugin( :clean,
          :filesets => [ { :directory => '${basedir}/ruby/gems/shared/specifications/default',
                           :includes => [ '*' ] },
                         { :directory => '${basedir}/ruby/stdlib',
                           :includes => [ '**/bouncycastle/**/*.jar' ] } ] )

  # tell maven to download the respective gem artifacts
  default_gems.each do |g|
    # use provided scope so it is not a real dependency for runtime
    dependency 'rubygems', g.name, g.version, :type => 'gem', :scope => :provided do
      exclusion 'rubygems:jar-dependencies'
    end
  end

  gem 'ruby-maven', '3.1.1.0.11', :scope => :provided

  default_gemnames = default_gems.collect { |g| g.name }

  # TODO no hardcoded group-ids
  plugin :dependency, :useRepositoryLayout => true, :outputDirectory => 'ruby/stdlib', :excludeGroupIds => 'rubygems', :includeScope => :provided do
    execute_goal 'copy-dependencies', :phase => 'generate-resources'
  end

  execute :install_gems, :'initialize' do |ctx|
    require 'fileutils'

    puts "using jruby #{JRUBY_VERSION}"

    target = ctx.project.build.directory.to_pathname
    gem_home = File.join( target, 'rubygems' )
    gems = File.join( gem_home, 'gems' )
    specs = File.join( gem_home, 'specifications' )
    cache = File.join( gem_home, 'cache' )
    jruby_gems = File.join( ctx.project.basedir.to_pathname, 'ruby', 'gems', 'shared' )
    default_specs = File.join( ctx.project.basedir.to_pathname, 'ruby', 'gems', 'shared',
                               'specifications', 'default' )
    bin_stubs = File.join( ctx.project.basedir.to_pathname, 'ruby', 'gems', 'shared',
                           'gems' )
    ruby_dir = File.join( ctx.project.basedir.to_pathname, 'ruby' )
    FileUtils.mkdir_p( default_specs )

    # have an empty openssl.rb so we do not run in trouble with not having
    # jopenssl which is part of the default gems
    lib_dir = File.join( target, 'lib' )
    openssl = File.join( lib_dir, 'openssl.rb' )
    FileUtils.mkdir_p( lib_dir )
    File.open( openssl, 'w' )
    $LOAD_PATH.unshift lib_dir

    # since the bouncy castle .jars are version-ed (e.g. bcprov-jdk15on-1.47)
    # make sure we cleanup before adding the ones from the jruby-openssl.gem:
    Dir.glob( File.join( lib_dir, "bc{prov,pkix}*.jar" ) ).each do |f|
      # use this instead of FileUtils.rm_f - issue #1698
      File.delete( f ) if File.exists?( f )
    end

    # now we can require the rubygems staff
    require 'rubygems/installer'
    require 'rubygems/package'

    puts 'install gems unless already installed'
    ENV_JAVA['jars.skip'] = 'true'
    ctx.project.artifacts.select do |a|
      a.group_id == 'rubygems' || a.group_id == 'org.jruby.gems'
    end.each do |a|
      ghome = default_gemnames.member?( a.artifact_id ) ? gem_home : jruby_gems
      if Dir[ File.join( ghome, 'cache', File.basename( a.file.to_pathname ).sub( /.gem/, '*.gem' ) ) ].empty?
        puts a.file.to_pathname
        # do not set bin_dir since its create absolute symbolic links
        installer = Gem::Installer.new( a.file.to_pathname,
                                        :ignore_dependencies => true,
                                        :install_dir => ghome )
        installer.install
      end
    end

    default_gems.each do |g|
      pom_version = ctx.project.properties.get( g.pom_version_key ) || g.pom_version_key
      version = pom_version.sub( /-SNAPSHOT/, '' )

      # install the gem unless already installed
      if Dir[ File.join( default_specs, "#{g.name}-#{version}*.gemspec" ) ].empty?

        puts
        puts "--- gem #{g.name}-#{version} ---"

        # copy the gem content to stdlib
        stdlib_dir = File.join( ruby_dir, 'stdlib' )
        puts "copy gem content to #{stdlib_dir}"
        # assume default require_path
        require_base = File.join( gems, "#{g.name}-#{version}*", 'lib' )
        require_files = File.join( require_base, '*' )

        # copy in new ones and mark writable for future updates (e.g. minitest)
        stdlib_locs = []
        Dir[ require_files ].each do |f|
          puts "copying: #{f} to #{stdlib_dir}"
          FileUtils.cp_r( f, stdlib_dir )

          stdlib_loc = f.sub( File.dirname(f), stdlib_dir )
          if File.directory?(stdlib_loc)
            stdlib_locs += Dir[stdlib_loc + "/*"].to_a
          else
            stdlib_locs << stdlib_loc
          end
        end

        # fix permissions on copied files
        stdlib_locs.each do |f|
          next if File.writable? f

          puts "fixing permissions: #{f}"
          # TODO: better way to just set it writable without changing all modes?
          FileUtils.chmod_R(0644, f)
        end

        # copy bin files if the gem has any
        bin = File.join( gems, "#{g.name}-#{version}", 'bin' )
        if File.exists? bin
          Dir[ File.join( bin, '*' ) ].each do |f|
            puts "copy to bin: #{File.basename( f )}"
            target = File.join( bin_stubs, f.sub( /#{gems}/, '' ) )
            FileUtils.mkdir_p( File.dirname( target ) )
            FileUtils.cp_r( f, target )
          end
        end

        if g.default_gem
          specfile_wildcard = "#{g.name}-#{version}*.gemspec"
          specfile = Dir[ File.join( specs,  specfile_wildcard ) ].first

          unless specfile
            raise Errno::ENOENT, "gemspec #{specfile_wildcard} not found in #{specs}; dependency unspecified in lib/pom.xml?"
          end

          specname = File.basename( specfile )
          puts "copy to specifications/default: #{specname}"

          spec = Gem::Package.new( Dir[ File.join( cache, "#{g.name}-#{version}*.gem" ) ].first ).spec
          File.open( File.join( default_specs, specname ), 'w' ) do |f|
            f.print( spec.to_ruby )
          end
        end
      end
    end

    # patch jruby-openssl - remove file which should be only inside gem
    # use this instead of FileUtils.rm_f - issue #1698
    f = File.join( ruby_dir, 'stdlib', 'jruby-openssl.rb' )
    File.delete( f ) if File.exists?( f )

    # we do not want rubygems_plugin.rb within jruby
    f = File.join( ruby_dir, 'stdlib', 'rubygems_plugin.rb' )
    File.delete( f ) if File.exists?( f )

    # fix file permissions of installed gems
    ( Dir[ File.join( jruby_gems, '**/*' ) ] + Dir[ File.join( jruby_gems, '**/.*' ) ] ).each do |f|
      File.chmod( 0644, f ) rescue nil if File.file?( f )
    end
  end

  execute( 'fix shebang on gem bin files and add *.bat files',
           'prepare-resources' ) do |ctx|
    
    puts 'fix the gem stub files'
    jruby_home = ctx.project.basedir.to_pathname + '/../'
    bindir = File.join( jruby_home, 'lib', 'ruby', 'gems', 'shared', 'bin' )
    Dir[ File.join( bindir, '*' ) ].each do |f|
      content = File.read( f )
      new_content = content.sub( /#!.*/, "#!/usr/bin/env jruby
" )
      File.open( f, "w" ) { |file| file.print( new_content ) }
    end
    
    puts 'generate the missing bat files'
    Dir[File.join( jruby_home, 'bin', '*' )].each do |fn|
      next unless File.file?(fn)
      next if fn =~ /.bat$/
      next if File.exist?("#{fn}.bat")
      next unless File.open(fn, 'r', :internal_encoding => 'ASCII-8BIT') do |io|
        line = io.readline rescue ""
        line =~ /^#!.*ruby/
      end
      puts "Generating #{File.basename(fn)}.bat"
      File.open("#{fn}.bat", "wb") do |f|
        f.print "@ECHO OFF\r\n"
        f.print "@\"%~dp0jruby.exe\" -S #{File.basename(fn)} %*\r\n"
      end
    end
  end
  
  execute( 'copy bin/jruby.bash to bin/jruby',
           'process-resources' ) do |ctx|
    require 'fileutils'
    jruby_complete = ctx.project.properties.get_property( 'jruby.complete.home' )
    FileUtils.cp( File.join( jruby_complete, 'bin', 'jruby.bash' ), 
                  File.join( jruby_complete, 'bin', 'jruby' ) )
  end

  execute 'jrubydir', 'prepare-package' do |ctx|
    require( ctx.project.basedir.to_pathname + '/../core/src/main/ruby/jruby/commands.rb' )
    JRuby::Commands.generate_dir_info( ctx.project.build.output_directory.to_pathname + '/META-INF/jruby.home' )
  end

  # we have no sources and attach an empty jar later in the build to
  # satisfy oss.sonatype.org upload

  plugin( :source, 'skipSource' =>  'true' )

  # this plugin is configured to attach empty jars for sources and javadocs
  plugin( 'org.codehaus.mojo:build-helper-maven-plugin' )

  build do

    # both resources are includes for the $jruby_home/lib directory

    resource do
      directory '${gem.home}'
      includes 'gems/rake-${rake.version}/bin/r*', 'gems/rdoc-${rdoc.version}/bin/r*', 'specifications/default/*.gemspec'
      target_path '${jruby.complete.gems}'
    end

    resource do
      directory '${basedir}/..'
      includes 'bin/ast*', 'bin/gem*', 'bin/irb*', 'bin/jgem*', 'bin/jirb*', 'bin/jruby*', 'bin/rake*', 'bin/ri*', 'bin/rdoc*', 'bin/testrb*', 'lib/ruby/stdlib/**', 'lib/ruby/truffle/**'
      excludes 'bin/jruby', 'bin/jruby*_*', 'bin/jruby*-*', '**/.*', 'lib/ruby/stdlib/rubygems/defaults/jruby_native.rb'
      target_path '${jruby.complete.home}'
    end
  end
end
