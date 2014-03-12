class ImportedGem
  attr_reader :name, :default_gem, :pom_version_key, :ruby_version, :only_spec

  def initialize( name, pom_version_key, default_gem, ruby_version = nil )
    @name = name
    @default_gem = default_gem
    @pom_version_key = pom_version_key
    @ruby_version = ruby_version
  end
end

# the versions are declared in ../pom.xml
default_gems = 
  [ 
   ImportedGem.new( 'jruby-openssl', 'jopenssl.version', true ),
   ImportedGem.new( 'rake', 'rake.version', true ),
   ImportedGem.new( 'rdoc', 'rdoc.version', true, '2.1' ),
   ImportedGem.new( 'json', 'json.version', true, '2.1' ),
   ImportedGem.new( 'krypt', 'krypt.version', true ),
   ImportedGem.new( 'krypt-core', 'krypt.version', true ),
   ImportedGem.new( 'krypt-provider-jdk', 'krypt.version', true ),
   ImportedGem.new( 'bouncy-castle-java', 'bc.version', true )
  ]

only_specs = [ 'jruby-openssl' ]

project 'JRuby Lib Setup' do

  version = '9000.dev' #File.read( File.join( basedir, '..', 'VERSION' ) )

  model_version '4.0.0'
  id "org.jruby:jruby-lib:#{version}"
  inherit "org.jruby:jruby-parent:#{version}"
  packaging 'pom'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'tesla.version' => '0.0.9',
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
    gem g.name, "${#{g.pom_version_key}}"
  end

  # this is not an artifact for maven central
  plugin :deploy, :skip => true 

  phase :package do
    plugin :dependency do
      items = default_gems.collect do |g|
        { 'groupId' =>  'rubygems',
          'artifactId' =>  g.name,
          'version' =>  "${#{g.pom_version_key}}",
          'type' =>  'gem',
          'overWrite' =>  'false',
          'outputDirectory' =>  '${project.build.directory}' }
      end
      execute_goals( 'copy',
                     :id => 'copy gems',
                     'artifactItems' => items )
    end
  end

  execute :install_gems, :package do |ctx|
    require 'fileutils'

    puts "using jruby #{JRUBY_VERSION}"

    target = ctx.project.build.directory.to_s
    gem_home = File.join( target, 'rubygems' )
    gems = File.join( gem_home, 'gems' )
    specs = File.join( gem_home, 'specifications' )
    default_specs = File.join( ctx.project.basedir.to_s, 'ruby', 'gems', 'shared', 
                               'specifications', 'default' )
    bin_stubs = File.join( ctx.project.basedir.to_s, 'ruby', 'gems', 'shared', 
                           'gems' )
    ruby_dir = File.join( ctx.project.basedir.to_s, 'ruby' )
    FileUtils.mkdir_p( default_specs )

    # have an empty openssl.rb so we do not run in trob=uble with not having
    # jopenssl which is part of the default gems
    openssl_dir = File.join( target, 'lib' )
    openssl = File.join( openssl_dir, 'openssl.rb' )
    FileUtils.mkdir_p( openssl_dir )
    File.open( openssl, 'w' )
    $LOAD_PATH.unshift openssl_dir

    # now we can require the rubygems staff
    require 'rubygems/installer'
    
    default_gems.each do |g|
      version = ctx.project.properties.get( g.pom_version_key )
      
      # install the gem unless already installed
      if Dir[ File.join( specs, "#{g.name}-#{version}*.gemspec" ) ].empty?
        installer = Gem::Installer.new( File.join( ctx.project.build.directory.to_s, 
                                                   "#{g.name}-#{version}.gem" ),
                                        :ignore_dependencies => true,
                                        :install_dir => gem_home )
        installer.install 

        puts
        puts "--- gem #{g.name}-#{version} ---"

        # copy the gem content to shared
        unless only_specs.include? g.name
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
        
        spec = Dir[ File.join( specs, "#{g.name}-#{version}*.gemspec" ) ].first
        puts "copy to specifications/default: #{File.basename( spec )}"
        FileUtils.cp( spec, default_specs )
      end
    end

    # patch the bouncy-castle loading problems on certain classloaders
    File.open( File.join( ruby_dir, 'shared', 'bouncy-castle-java.rb' ), 'w' ) do |f|
      bc_version = ctx.project.properties.get( 'bouncy-castle.version' )
      f.puts "require 'bcpkix-jdk15on-#{bc_version}.jar'"
      f.puts "require 'bcprov-jdk15on-#{bc_version}.jar'"
    end
  end
end
