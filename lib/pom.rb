class ImportedGem
  attr_reader :name,  :version

  def initialize( name, version )
    @name = name
    @version = version
  end

end

default_gems =
  [
   ImportedGem.new( 'jruby-openssl', '0.9.19' ),
   ImportedGem.new( 'rake', '${rake.version}' ),
   ImportedGem.new( 'rdoc', '${rdoc.version}' ),
   ImportedGem.new( 'json', '1.8.0' ),
   ImportedGem.new( 'jar-dependencies', '${jar-dependencies.version}' )
  ]

project 'JRuby Lib Setup' do

  version = File.read( File.join( basedir, '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id 'jruby-lib'
  inherit "org.jruby:jruby-parent", version
  packaging 'pom'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'tesla.version' => '0.1.1',
              'jruby.plugins.version' => '1.1.2' )

  # just depends on jruby-core so we are sure the jruby.jar is in place
  jar "org.jruby:jruby-core:#{version}"

  # for testing out jruby-ossl before final release :
  #repository( :url => 'http://oss.sonatype.org/content/repositories/staging',
  #            :id => 'gem-staging' )

  plugin( :clean,
          :filesets => [ { :directory => '${basedir}/ruby/gems/shared/specifications/default',
                           :includes => [ '*' ] },
                         { :directory => '${basedir}/ruby/shared',
                           :includes => [ 'org/**/*.jar' ] } ] )

  # tell maven to download the respective gem artifacts
  default_gems.each do |g|
    gem g.name, g.version
  end

  # this is not an artifact for maven central
  plugin :deploy, :skip => true

  execute :install_gems, :package do |ctx|
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
    ctx.project.artifacts.select do |a|
      a.group_id == 'rubygems'
    end.each do |a|
      ghome = a.scope == 'compile' ? gem_home : jruby_gems
      if Dir[ File.join( ghome, 'cache', File.basename( a.file.to_pathname ).sub( /.gem/, '*.gem' ) ) ].empty?
        puts a.file.to_pathname
        installer = Gem::Installer.new( a.file.to_pathname,
                                        :wrappers => true,
                                        :ignore_dependencies => true,
                                        :install_dir => ghome )
        installer.install
      end
    end

    default_gems.each do |g|
      pom_version = ctx.project.properties.get( g.version[2..-2] ) || g.version
      version = pom_version.sub( /-SNAPSHOT/, '' )

      # install the gem unless already installed
      if Dir[ File.join( default_specs, "#{g.name}-#{version}*.gemspec" ) ].empty?

        puts
        puts "--- gem #{g.name}-#{version} ---"

        # copy the gem content to shared or to respective
        puts "copy gem content to ruby/shared"
        # assume default require_path
        Dir[ File.join( gems, "#{g.name}-#{version}*", 'lib', '*' ) ].each do |f|
          FileUtils.cp_r( f, File.join( ruby_dir, "shared" ) )
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

        specname = File.basename( Dir[ File.join( specs, "#{g.name}-#{version}*.gemspec" ) ].first )
        puts "copy to specifications/default: #{specname}"

        spec = Gem::Package.new( Dir[ File.join( cache, "#{g.name}-#{version}*.gem" ) ].first ).spec
        File.open( File.join( default_specs, specname ), 'w' ) do |f|
          f.print( spec.to_ruby )
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

    # fix file permissions of installed gems
    ( Dir[ File.join( jruby_gems, '**/*' ) ] + Dir[ File.join( jruby_gems, '**/.*' ) ] ).each do |f|
      File.chmod( 0644, f ) rescue nil if File.file?( f )
    end
  end
end
