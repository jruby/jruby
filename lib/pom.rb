MORE_QUIET = ENV['JRUBY_BUILD_MORE_QUIET']

if MORE_QUIET
  class Gem::Installer
    def say(message)
      if message != spec.post_install_message || !MORE_QUIET
        super
      end
    end
  end
end

def log(message=nil)
  puts message unless MORE_QUIET
end

class ImportedGem
  attr_reader :name, :version, :default_spec

  def initialize( name, version, default_spec = true )
    @name = name
    @version = version
    @default_spec = default_spec
  end
end

default_gems =
  [
   ImportedGem.new( 'jruby-openssl', '0.9.19' ),
   ImportedGem.new( 'jruby-readline', '1.1.1' ),
   ImportedGem.new( 'rake', '${rake.version}' ),
   ImportedGem.new( 'rdoc', '${rdoc.version}' ),
   ImportedGem.new( 'minitest', '${minitest.version}' ),
   ImportedGem.new( 'test-unit', '${test-unit.version}' ),
   ImportedGem.new( 'power_assert', '${power_assert.version}' ),
   ImportedGem.new( 'psych', '2.0.17' ),
   ImportedGem.new( 'json', '${json.version}' ),
   ImportedGem.new( 'jar-dependencies', '${jar-dependencies.version}' ),
   ImportedGem.new( 'racc', '${racc.version}'),
   ImportedGem.new( 'net-telnet', '0.1.1'),
   ImportedGem.new( 'did_you_mean', '1.0.1'),
  ]

project 'JRuby Lib Setup' do

  version = ENV['JRUBY_VERSION'] ||
    File.read( File.join( basedir, '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id 'jruby-stdlib'
  inherit "org.jruby:jruby-parent", version

  properties( 'polyglot.dump.pom' => 'pom.xml',
              'polyglot.dump.readonly' => true,
              'jruby.plugins.version' => '1.1.2',
              'gem.home' => '${basedir}/ruby/gems/shared',
              # we copy everything into the target/classes/META-INF
              # so the jar plugin just packs it - see build/resources below
              'jruby.complete.home' => '${project.build.outputDirectory}/META-INF/jruby.home',
              'jruby.complete.gems' => '${jruby.complete.home}/lib/ruby/gems/shared' )

  # just depends on jruby-core so we are sure the jruby.jar is in place
  jar "org.jruby:jruby-core:#{version}", :scope => 'test'

  extension 'org.torquebox.mojo:mavengem-wagon:0.2.0'

  repository :id => :mavengems, :url => 'mavengem:https://rubygems.org'

  # for testing out jruby-ossl before final release :
  #repository( :url => 'https://oss.sonatype.org/content/repositories/snapshots',
  #            :id => 'gem-snaphots' )
  repository( :url => 'http://oss.sonatype.org/content/repositories/staging',
              :id => 'gem-staging' )

  plugin( :clean,
          :filesets => [ { :directory => '${basedir}/ruby/gems/shared/specifications/default',
                           :includes => [ '*' ] },
                         { :directory => '${basedir}/ruby/stdlib',
                           :includes => [ 'org/**/*.jar' ] } ] )

  # tell maven to download the respective gem artifacts
  default_gems.each do |g|
    # use provided scope so it is not a real dependency for runtime
    dependency 'rubygems', g.name, g.version, :type => 'gem', :scope => :provided do
      exclusion 'rubygems:jar-dependencies'
    end
  end

  default_gemnames = default_gems.collect { |g| g.name }

  plugin :dependency,
    :useRepositoryLayout => true,
    :outputDirectory => 'ruby/stdlib',
    :excludeGroupIds => 'rubygems',
    :includeScope => :provided do
    execute_goal 'copy-dependencies', :phase => 'generate-resources'
  end

  execute :install_gems, :'initialize' do |ctx|
    require 'fileutils'

    log "using jruby #{JRUBY_VERSION}"

    target = ctx.project.build.directory.to_pathname
    gem_home = File.join( target, 'rubygems' )
    gems = File.join( gem_home, 'gems' )
    specs = File.join( gem_home, 'specifications' )
    cache = File.join( gem_home, 'cache' )
    jruby_gems = File.join( ctx.project.basedir.to_pathname, 'ruby', 'gems', 'shared' )
    default_specs = File.join( jruby_gems, 'specifications', 'default' )
    bin_stubs = File.join( jruby_gems, 'gems' )
    ruby_dir = File.join( ctx.project.basedir.to_pathname, 'ruby' )
    stdlib_dir = File.join( ruby_dir, 'stdlib' )

    FileUtils.mkdir_p( default_specs )

    # have an empty openssl.rb so we do not run in trouble with not having
    # jopenssl which is part of the default gems
    lib_dir = File.join( target, 'lib' )
    openssl = File.join( lib_dir, 'openssl.rb' )
    FileUtils.mkdir_p( lib_dir )
    FileUtils.touch( openssl )
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

    log 'install gems unless already installed'
    ENV_JAVA['jars.skip'] = 'true'
    ctx.project.artifacts.select do |a|
      a.group_id == 'rubygems' || a.group_id == 'org.jruby.gems'
    end.each do |a|
      ghome = default_gemnames.member?( a.artifact_id ) ? gem_home : jruby_gems
      if Dir[ File.join( ghome, 'cache', File.basename( a.file.to_pathname ).sub( /.gem/, '*.gem' ) ) ].empty?
        log a.file.to_pathname
        installer = Gem::Installer.new( a.file.to_pathname,
                                        :wrappers => true,
                                        :ignore_dependencies => true,
                                        :install_dir => ghome )
        def installer.ensure_required_ruby_version_met; end
        installer.install
      end
    end

    default_gems.each do |g|
      pom_version = ctx.project.properties.get( g.version[2..-2] ) || g.version
      version = pom_version.sub( /-SNAPSHOT/, '' )

      # install the gem unless already installed
      if Dir[ File.join( default_specs, "#{g.name}-#{version}*.gemspec" ) ].empty?

        log
        log "--- gem #{g.name}-#{version} ---"

        # copy the gem content to stdlib

        log "copy gem content to #{stdlib_dir}"
        # assume default require_path
        require_base = File.join( gems, "#{g.name}-#{version}*", 'lib' )
        require_files = File.join( require_base, '*' )

        # copy in new ones and mark writable for future updates (e.g. minitest)
        stdlib_locs = Dir[ require_files ].map do |f|
          log " copying: #{f} to #{stdlib_dir}" if $VERBOSE
          FileUtils.cp_r( f, stdlib_dir )

          stdlib_loc = f.sub( File.dirname(f), stdlib_dir )
          File.directory?(stdlib_loc) ? Dir[stdlib_loc + "/*"].to_a : stdlib_loc
        end
        stdlib_locs.flatten!

        # fix permissions on copied files
        stdlib_locs.each do |f|
          next if File.writable? f
          log " fixing permissions: #{f}" if $VERBOSE
          # TODO: better way to just set it writable without changing all modes?
          FileUtils.chmod_R(0644, f)
        end

        # copy bin files if the gem has any
        bin = File.join( gems, "#{g.name}-#{version}", 'bin' )
        if File.exists? bin
          Dir[ File.join( bin, '*' ) ].each do |f|
            log "copy to bin: #{File.basename( f )}"
            target = File.join( bin_stubs, f.sub( /#{gems}/, '' ) )
            FileUtils.mkdir_p( File.dirname( target ) )
            FileUtils.cp_r( f, target )
          end
        end

        if g.default_spec
          specfile_wildcard = "#{g.name}-#{version}*.gemspec"
          specfile = Dir[ File.join( specs,  specfile_wildcard ) ].first

          unless specfile
            raise Errno::ENOENT, "gemspec #{specfile_wildcard} not found in #{specs}; dependency unspecified in lib/pom.xml?"
          end

          specname = File.basename( specfile )
          log "copy to specifications/default: #{specname}"

          spec = Gem::Package.new( Dir[ File.join( cache, "#{g.name}-#{version}*.gem" ) ].first ).spec
          File.open( File.join( default_specs, specname ), 'w' ) do |f|
            f.print( spec.to_ruby )
          end
        end
      end
    end

    # patch jruby-openssl - remove file which should be only inside gem
    # use this instead of FileUtils.rm_f - issue #1698
    f = File.join( stdlib_dir, 'jruby-openssl.rb' )
    File.delete( f ) if File.exists?( f )

    # we do not want rubygems_plugin.rb within jruby
    f = File.join( stdlib_dir, 'rubygems_plugin.rb' )
    File.delete( f ) if File.exists?( f )

    # fix file permissions of installed gems
    ( Dir[ File.join( jruby_gems, '**/*' ) ] + Dir[ File.join( jruby_gems, '**/.*' ) ] ).each do |f|
      File.chmod( 0644, f ) rescue nil if File.file?( f )
    end
  end

  execute( 'fix shebang on gem bin files and add *.bat files',
           'generate-resources' ) do |ctx|

    log 'fix the gem stub files'
    jruby_home = ctx.project.basedir.to_pathname + '/../'
    bindir = File.join( jruby_home, 'lib', 'ruby', 'gems', 'shared', 'bin' )
    Dir[ File.join( bindir, '*' ) ].each do |f|
      content = File.read( f )
      new_content = content.sub( /#!.*/, "#!/usr/bin/env jruby
" )
      File.open( f, "w" ) { |file| file.print( new_content ) }
    end

    log 'generating missing .bat files'
    Dir[File.join( jruby_home, 'bin', '*' )].each do |fn|
      next unless File.file?(fn)
      next if fn =~ /.bat$/
      next if File.exist?("#{fn}.bat")
      next unless File.open(fn, 'r', :internal_encoding => 'ASCII-8BIT') do |io|
        line = io.readline rescue ""
        line =~ /^#!.*ruby/
      end
      log " generating #{File.basename(fn)}.bat" if $VERBOSE
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

  plugin( :source, 'skipSource' => 'true' )

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
      includes 'bin/ast*', 'bin/gem*', 'bin/irb*', 'bin/jgem*', 'bin/jirb*', 'bin/jruby*', 'bin/rake*', 'bin/ri*', 'bin/rdoc*', 'bin/testrb*', 'lib/ruby/include/**', 'lib/ruby/stdlib/**', 'lib/ruby/truffle/**'
      excludes 'bin/jruby', 'bin/jruby*_*', 'bin/jruby*-*', '**/.*',
        'lib/ruby/stdlib/rubygems/defaults/jruby_native.rb',
        'lib/ruby/stdlib/gauntlet*.rb' # gauntlet_rdoc.rb, gauntlet_rubygems.rb
      target_path '${jruby.complete.home}'
    end

    resource do
      directory '${project.basedir}/..'
      includes [ 'BSDL', 'COPYING', 'LEGAL', 'LICENSE.RUBY' ]
      target_path '${project.build.outputDirectory}/META-INF/'
    end
  end
end
