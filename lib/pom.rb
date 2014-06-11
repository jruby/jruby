class ImportedGem
  attr_reader :name, :default_gem, :pom_version_key, :ruby_version, :only_spec

  def initialize( name, pom_version_key, default_gem, ruby_version = nil, only_spec = false )
    @name = name
    @default_gem = default_gem
    @pom_version_key = pom_version_key
    if ( ruby_version == false )
      @only_specs = true
    else
      @ruby_version = ruby_version
    end
    @only_spec = only_spec
  end

  def group_id
    if name.match( /^jruby-/ ) && pom_version_key.match( /-SNAPSHOT$/ )
      'org.jruby.gems'
    else
      'rubygems'
    end
  end

  def version
    if pom_version_key =~ /.version/
      "${#{pom_version_key}}"
    else
      pom_version_key
    end
  end
end
require 'rexml/document'
require 'rexml/xpath'

KRYPT_VERSION = '0.0.2'

# the versions are declared in ../pom.xml
default_gems =
  [
   ImportedGem.new( 'jruby-openssl', '0.9.5', true ),
   ImportedGem.new( 'rake', 'rake.version', true ),
   ImportedGem.new( 'rdoc', 'rdoc.version', true, false, true ),
   ImportedGem.new( 'json', 'json.version', true, false ),
   ImportedGem.new( 'krypt', KRYPT_VERSION, true ),
   ImportedGem.new( 'krypt-core', KRYPT_VERSION, true ),
   ImportedGem.new( 'krypt-provider-jdk', KRYPT_VERSION, true ),
   ImportedGem.new( 'ffi', '1.9.3', true )
   ImportedGem.new( 'jar-dependencies', '0.0.3', true ),
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

  doc = REXML::Document.new File.new(File.join(File.join( basedir, 'pom.xml')))
  version = REXML::XPath.first(doc, "//project/parent/version").text

  model_version '4.0.0'
  id 'jruby-lib'
  inherit "org.jruby:jruby-parent", version
  packaging 'pom'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'tesla.version' => '0.1.0',
              'jruby.home' => '${basedir}/..' )

  # just depends on jruby-core so we are sure the jruby.jar is in place
  jar "org.jruby:jruby-core:#{version}"

  repository( 'http://rubygems-proxy.torquebox.org/releases',
              :id => 'rubygems-releases' )

  plugin( :clean,
          :filesets => [ { :directory => '${basedir}/ruby/gems/shared/specifications/default',
                           :includes => [ '*' ] } ] )

  # tell maven to download the respective gem artifacts
  default_gems.each do |g|
    if g.group_id != 'rubygems'
      dependency g.group_id, g.name, g.pom_version_key, :type => :gem
    else
      gem g.name, g.version
    end
  end

  # this is not an artifact for maven central
  plugin :deploy, :skip => true

  phase :package do
    plugin :dependency do
      items = default_gems.collect do |g|
        { 'groupId' =>  g.group_id,
          'artifactId' =>  g.name,
          'version' =>  g.version,
          'type' =>  'gem',
          'overWrite' =>  'false',
          'outputDirectory' =>  '${project.build.directory}' }
      end
      execute_goals( 'copy',
                     :id => 'copy gems',
                     :useBaseVersion => true,
                     'artifactItems' => items )
    end
  end

  execute :install_gems, :package do |ctx|
    require 'fileutils'

    puts "using jruby #{JRUBY_VERSION}"

    target = ctx.project.build.directory.to_pathname
    gem_home = File.join( target, 'rubygems' )
    gems = File.join( gem_home, 'gems' )
    specs = File.join( gem_home, 'specifications' )
    cache = File.join( gem_home, 'cache' )
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

    default_gems.each do |g|
      pom_version = ctx.project.properties.get( g.pom_version_key ) || g.pom_version_key
      version = pom_version.sub( /-SNAPSHOT/, '' )

      # install the gem unless already installed
      if Dir[ File.join( specs, "#{g.name}-#{version}*.gemspec" ) ].empty?
        installer = Gem::Installer.new( File.join( ctx.project.build.directory.to_pathname,
                                                   "#{g.name}-#{pom_version}.gem" ),
                                        :ignore_dependencies => true,
                                        :install_dir => gem_home )
        installer.install

        puts
        puts "--- gem #{g.name}-#{version} ---"

        unless g.only_spec
          # copy the gem content to shared or to respective
          dir = g.ruby_version || 'shared'
          puts "copy gem content to ruby/#{dir}"
          # assume default require_path
          Dir[ File.join( gems, "#{g.name}-#{version}*", 'lib', '*' ) ].each do |f|
            FileUtils.cp_r( f, File.join( ruby_dir, dir ) )
          end
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
          specname = File.basename( Dir[ File.join( specs, "#{g.name}-#{version}*.gemspec" ) ].first )
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
    f = File.join( ruby_dir, 'shared', 'jruby-openssl.rb' )
    File.delete( f ) if File.exists?( f )

    # we do not want rubygems_plugin.rb within jruby
    f = File.join( ruby_dir, 'shared', 'rubygems_plugin.rb' )
    File.delete( f ) if File.exists?( f )
  end
end
