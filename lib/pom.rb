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
   ImportedGem.new( 'jruby-openssl', '0.9.6', true ),
   ImportedGem.new( 'jruby-readline', '1.0', false ),
   ImportedGem.new( 'rake', 'rake.version', true ),
   ImportedGem.new( 'rdoc', 'rdoc.version', true ),
   ImportedGem.new( 'json', 'json.version', true ),
   ImportedGem.new( 'jar-dependencies', '0.1.8', true ),
   ImportedGem.new( 'minitest', 'minitest.version', true ),
   ImportedGem.new( 'test-unit', 'test-unit.version', true ),
   ImportedGem.new( 'power_assert', 'power_assert.version', true ),
   ImportedGem.new( 'psych', '2.0.9-SNAPSHOT', true )
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
  id 'jruby-lib'
  inherit "org.jruby:jruby-parent", version
  packaging 'pom'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'tesla.version' => '0.1.1',
              'jruby.plugins.version' => '1.0.5' )

  unless version =~ /-SNAPSHOT/
    properties 'jruby.home' => '${basedir}/..'
  end

  # just depends on jruby-core so we are sure the jruby.jar is in place
  jar "org.jruby:jruby-core:#{version}", :scope => 'provided'

  repository( :url => 'http://rubygems-proxy.torquebox.org/releases',
              :id => 'rubygems-releases' )

  plugin( :clean,
          :filesets => [ { :directory => '${basedir}/ruby/gems/shared/specifications/default',
                           :includes => [ '*' ] },
                         { :directory => '${basedir}/ruby/stdlib',
                           :includes => [ '**/bouncycastle/**/*.jar' ] } ] )

  # tell maven to download the respective gem artifacts
  default_gems.each do |g|
    dependency 'rubygems', g.name, g.version, :type => 'gem' do
      exclusion 'rubygems:jar-dependencies'
    end
  end

  # this is not an artifact for maven central
  plugin :deploy, :skip => true

  plugin :invoker, :skipInstallation => true

  gem 'ruby-maven', '3.1.1.0.8', :scope => :provided

  plugin :dependency, :useRepositoryLayout => true, :outputDirectory => 'ruby/stdlib', :excludeGroupIds => 'rubygems', :includeScope => :runtime do
    execute_goal 'copy-dependencies', :phase => 'package'
  end

  execute :install_gems, :'package' do |ctx|
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

    # have an empty openssl.rb so we do not run in trob=uble with not having
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
      ghome = a.scope == 'compile' ? gem_home : jruby_gems
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
            raise Errno::ENOENT, "gemspec #{specfile_wildcard} not found; dependency unspecified in lib/pom.xml?"
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
end
