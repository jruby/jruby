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

default_gems = [
    # treat RGs update special:
    # - we do not want bin/update_rubygems or bin/gem overrides
    ['rubygems-update', '3.6.3', { bin: false, require_paths: ['lib'] }],
    ['abbrev', '0.1.0'],
    ['base64', '0.1.1'],
    ['benchmark', '0.2.0'],
    # Extension still lives in JRuby. See https://github.com/ruby/bigdecimal/issues/268
    ['bigdecimal', '3.1.4'],
    ['bundler', '2.6.3'],
    ['cgi', '0.3.6'],
    ['csv', '3.2.5'],
    # Currently using a stub gem for JRuby until we can incorporate our code.
    # https://github.com/ruby/date/issues/48
    ['date', '3.3.3'],
    # Newer versions require deep control over CRuby, needs work to support JRuby.
    # See bundled gems below.
    ['debug', '0.2.1'],
    ['delegate', '0.2.0'],
    ['did_you_mean', '1.6.1'],
    ['digest', '3.1.0'],
    ['drb', '2.1.0'],
    ['english', '0.7.1'],
    ['erb', '2.2.3'],
    ['error_highlight', '0.3.0'],
    # https://github.com/ruby/etc/issues/19
    # ['etc', '1.3.0'],
    # https://github.com/ruby/fcntl/issues/9
    # ['fcntl', '1.0.1'],
    ['ffi', '1.16.3'],
    ['fiddle', '1.1.4'],
    ['fileutils', '1.6.0'],
    ['find', '0.1.1'],
    ['forwardable', '1.3.2'],
    # ['gdbm', '2.1.0'],
    ['getoptlong', '0.1.1'],
    ['io-console', '0.7.2'],
    # https://github.com/ruby/io-nonblock/issues/4
    # ['io-nonblock', '0.1.0'],
    ['io-wait', '0.3.0'],
    ['ipaddr', '1.2.4'],
    ['irb', '1.4.2'],
    ['jar-dependencies', '0.5.4'],
    ['jruby-readline', '1.3.7'],
    ['jruby-openssl', '0.15.3'],
    ['json', '2.7.1'],
    ['logger', '1.5.1'],
    ['mutex_m', '0.1.1'],
    ['net-http', '0.3.0'],
    ['net-protocol', '0.1.2'],
    ['nkf', '0.2.0'],
    ['observer', '0.1.1'],
    ['open3', '0.1.2'],
    # https://github.com/ruby/openssl/issues/20#issuecomment-1022872855
    # ['openssl', '3.0.0'],
    ['open-uri', '0.3.0'],
    ['optparse', '0.2.0'],
    ['ostruct', '0.5.5'],
    # https://github.com/ruby/pathname/issues/17
    # ['pathname', '0.2.0'],
    ['pp', '0.3.0'],
    ['prettyprint', '0.1.1'],
    ['pstore', '0.1.1'],
    ['psych', '5.2.3'],
    ['racc', '1.6.0'],
    ['rake-ant', '1.0.6'],
    ['rdoc', '6.4.1.1'],
    # https://github.com/ruby/readline/issues/5
    # ['readline', '0.0.3'],
    # Will be solved with readline
    # ['readline-ext', '0.1.4'],
    ['reline', '0.5.12'],
    # https://github.com/ruby/resolv/issues/19
    # https://github.com/ruby/resolv/pull/75
    # ['resolv', '0.2.1'],
    ['resolv-replace', '0.1.0'],
    ['rinda', '0.1.1'],
    ['ruby2_keywords', '0.0.5'],
    ['securerandom', '0.2.0'],
    # https://github.com/ruby/set/issues/21
    # ['set', '1.0.2'],
    ['shellwords', '0.1.0'],
    ['singleton', '0.1.1'],
    ['stringio', '3.1.2'],
    ['strscan', '3.1.0'],
    ['subspawn', '0.1.1'], # has 3 transitive deps:
      ['subspawn-posix', '0.1.1'],
      ['ffi-binary-libfixposix', '0.5.1.1'],
      ['ffi-bindings-libfixposix', '0.5.1.0'],
    # https://github.com/ruby/syslog/issues/1
    # ['syslog', '0.1.0'],
    # https://github.com/ruby/tempfile/issues/7
    # ['tempfile', '0.1.2'],
    ['time', '0.2.2'],
    ['timeout', '0.3.2'],
    # https://github.com/ruby/tmpdir/issues/13
    # ['tmpdir', '0.1.2'],
    ['tsort', '0.1.0'],
    ['un', '0.2.0'],
    ['uri', '0.12.2'],
    ['weakref', '0.1.1'],
    # https://github.com/ruby/win32ole/issues/12
    # ['win32ole', '1.8.8'],
    ['yaml', '0.2.0'],
    # https://github.com/ruby/zlib/issues/38
    # ['zlib', '2.1.1'],
]

bundled_gems = [
    # Depends on many CRuby internals
    # ['debug', '1.4.0'],
    ['matrix', '0.4.2'],
    ['minitest', '5.15.0'],
    ['net-ftp', '0.3.7'],
    ['net-imap', '0.2.3'],
    ['net-pop', '0.1.1'],
    ['net-smtp', '0.3.1'],
    ['prime', '0.1.2'],
    ['power_assert', '2.0.1'],
    ['rake', '${rake.version}'],
    # Depends on many CRuby internals
    # ['rbs', '2.0.0'],
    ['rexml', '3.3.9'],
    ['rss', '0.2.9'],
    ['test-unit', '3.5.3'],
    # Depends on many CRuby internals
    # ['typeprof', '0.21.1'],
]

project 'JRuby Lib Setup' do

  version = ENV['JRUBY_VERSION'] ||
    File.read( File.join( basedir, '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id 'jruby-stdlib'
  inherit "org.jruby:jruby-parent", version

  properties( 'polyglot.dump.pom' => 'pom.xml',
              'polyglot.dump.readonly' => true,
              'jruby.plugins.version' => '3.0.5',
              'gem.home' => '${basedir}/ruby/gems/shared',
              # we copy everything into the target/classes/META-INF
              # so the jar plugin just packs it - see build/resources below
              'jruby.complete.home' => '${project.build.outputDirectory}/META-INF/jruby.home',
              'jruby.complete.gems' => '${jruby.complete.home}/lib/ruby/gems/shared' )

  # just depends on jruby-core so we are sure the jruby.jar is in place
  jar "org.jruby:jruby-core:#{version}", :scope => 'test'

  extension 'org.jruby.maven:mavengem-wagon:2.0.2'

  repository :id => :mavengems, :url => 'mavengem:https://rubygems.org'

  # for testing out jruby-ossl before final release :
  # repository :id => 'gem-snaphots', :url => 'https://oss.sonatype.org/content/repositories/snapshots'
  # repository :id => 'gem-staging', :url => 'http://oss.sonatype.org/content/repositories/staging'

  plugin( :clean,
          :filesets => [ { :directory => '${basedir}/ruby/gems/shared/specifications/default',
                           :includes => [ '*' ] },
                         { :directory => '${basedir}/ruby/stdlib',
                           :includes => [ 'org/**/*.jar' ] } ] )

  # tell maven to download the respective gem artifacts
  default_gems.each do |name, version|
    # use provided scope so it is not a real dependency for runtime
    dependency 'rubygems', name, version, :type => 'gem', :scope => :provided do
      exclusion 'rubygems:jar-dependencies'
    end
  end

  bundled_gems.each do |name, version|
    # use provided scope so it is not a real dependency for runtime
    dependency 'rubygems', name, version, :type => 'gem', :scope => :provided do
      exclusion 'rubygems:jar-dependencies'
    end
  end

  default_gemnames = default_gems.collect(&:first)
  all_gems     = default_gems + bundled_gems

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
    bin_stubs = File.join( jruby_gems, 'gems' )
    default_specs = File.join( jruby_gems, 'specifications', 'default' )
    ruby_dir = File.join( ctx.project.basedir.to_pathname, 'ruby' )
    stdlib_dir = File.join( ruby_dir, 'stdlib' )
    jruby_home = ctx.project.parent.basedir.to_pathname

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

    # bin location for global binstubs
    global_bin = File.join( jruby_home, "bin" )

    # force Ruby command to "jruby" for the generated Windows bat files since we install using 9.1.17.0 jar file
    Gem.singleton_class.send(:define_method, :ruby) do
      File.join(global_bin, "jruby#{RbConfig::CONFIG['EXEEXT']}")
    end

    # Disable extension build for gems (none of ours require a build)
    class Gem::Ext::Builder
      def build_extensions
        return if @spec.extensions.empty?

        say "Skipping native extensions."

        FileUtils.mkdir_p File.dirname(@spec.gem_build_complete_path)
        FileUtils.touch @spec.gem_build_complete_path
      end
    end

    ctx.project.artifacts.select do |a|
      a.group_id == 'rubygems' || a.group_id == 'org.jruby.gems'
    end.each do |a|
      ghome = default_gemnames.member?( a.artifact_id ) ? gem_home : jruby_gems
      if Dir[ File.join( ghome, 'cache', File.basename( a.file.to_pathname ).sub( /.gem/, '*.gem' ) ) ].empty?
        log a.file.to_pathname
        installer = Gem::Installer.new( Gem::Package.new(a.file.to_pathname),
                                        wrappers: true,
                                        ignore_dependencies: true,
                                        install_dir: ghome,
                                        env_shebang: true )
        def installer.ensure_required_ruby_version_met; end
        installer.install
      end
    end

    copy_gem_executables = lambda do |spec, gem_home|
      if !spec.executables.empty?
        bin_source = Gem.bindir(gem_home) # Gem::Installer generated bin scripts here
        spec.executables.each do |file|
          source = File.expand_path(file, bin_source)
          target = File.join(jruby_home, 'bin') # JRUBY_HOME/bin binstubs
          log "copy executable #{source} to #{target}"
          FileUtils.cp(source, target)
        end
      end
    end

    default_gems.each do |name, version, options|
      version = ctx.project.properties.get(version[2..-2]) || version # resolve ${xyz.version} declarations
      version = version.sub( /-SNAPSHOT/, '' )
      gem_name = "#{name}-#{version}"
      options = { bin: true, spec: true }.merge(options || {})

      # install the gem unless already installed
      if Dir[ File.join( default_specs, "#{gem_name}*.gemspec" ) ].empty?

        log
        log "--- gem #{gem_name} ---"

        # copy the gem content to stdlib

        log "copy gem content to #{stdlib_dir}"

        spec = Gem::Package.new( Dir[ File.join( cache, "#{gem_name}*.gem" ) ].first ).spec

        require_paths = options[:require_paths] || spec.require_paths

        require_paths.each do |require_path|
          require_base = File.join( gems, "#{gem_name}*", require_path )
          require_files = File.join( require_base, '*' )

          # copy in new ones and mark writable for future updates (e.g. minitest)
          stdlib_locs = Dir[ require_files ].map do |f|
            log " copying: #{f} to #{stdlib_dir}"# if $VERBOSE
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
        end

        # get gemspec
        specfile_wildcard = "#{gem_name}*.gemspec"
        specfile = Dir[ File.join( specs,  specfile_wildcard ) ].first

        unless specfile
          raise Errno::ENOENT, "gemspec #{specfile_wildcard} not found in #{specs}; dependency unspecified in lib/pom.xml?"
        end

        # copy bin files if the gem has any
        copy_gem_executables.call(spec, gem_home) if options[:bin]

        # TODO: try avoiding these binstub of gems - should use a full gem location
        spec.executables.each do |f|
          bin = Dir.glob(File.join( gems, "#{gem_name}*", spec.bindir ))[0]
          source = File.join( bin, f )
          target = File.join( bin_stubs, source.sub( gems, '' ) )
          log "copy #{f} to #{target}"
          FileUtils.mkdir_p( File.dirname( target ) )
          FileUtils.cp_r( source, target )
        end

        if options[:spec]
          specname = File.basename( specfile )
          log "copy to specifications/default: #{specname}"
          File.open( File.join( default_specs, specname ), 'w' ) do |f|
            f.print( spec.to_ruby )
          end
        end
      end
    end

    bundled_gems.each do |name, version| # copy bin files for bundled gems (e.g. rake) as well
      version = ctx.project.properties.get(version[2..-2]) || version # e.g. resolve '${rake.version}' from properties
      gem_name = "#{name}-#{version}"
      spec = Gem::Package.new( Dir[ File.join(jruby_gems, "cache", "#{gem_name}*.gem" ) ].first ).spec
      copy_gem_executables.call(spec, jruby_gems)
    end

    # patch jruby-openssl - remove file which should be only inside gem
    # use this instead of FileUtils.rm_f - issue #1698
    f = File.join( stdlib_dir, 'jruby-openssl.rb' )
    File.delete( f ) if File.exists?( f )

    # we do not want rubygems_plugin.rb within jruby
    f = File.join( stdlib_dir, 'rubygems_plugin.rb' )
    File.delete( f ) if File.exists?( f )

    # axiom-types appears to be a dead project but a transitive dep we still
    # have.  It contains unreadable files which messes up some upstream
    # maintainers like OpenBSD (see #1989).
    hack = File.join jruby_gems, 'gems', 'axiom-types-*'
    (Dir[File.join(hack, '**/*')] + Dir[File.join(hack, '**/.*' )]).each do |f|
      FileUtils.chmod 'u+rw,go+r' rescue nil if File.file?(f)
    end
  end

  execute( 'fix shebang on gem bin files and add *.bat files',
           'generate-resources' ) do |ctx|

    log 'generating missing .bat files'
    jruby_home = ctx.project.parent.basedir.to_pathname
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

  execute( 'copy bin/jruby.sh to bin/jruby',
           'process-resources' ) do |ctx|
    require 'fileutils'
    jruby_complete = ctx.project.properties.get_property( 'jruby.complete.home' )
    FileUtils.cp( File.join( jruby_complete, 'bin', 'jruby.sh' ),
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
    resource do
      directory '${gem.home}'
      includes [
                   'specifications/default/*',
                   *all_gems.map {|name,version| "specifications/#{name}-#{version}*"},
                   *all_gems.map {|name,version| "gems/#{name}-#{version}*/**/*"},
                   *all_gems.map {|name,version| "cache/#{name}-#{version}*"},
               ]
      target_path '${jruby.complete.gems}'
    end

    resource do
      directory '${basedir}/..'
      includes 'bin/*', 'lib/ruby/include/**', 'lib/ruby/stdlib/**'
      excludes 'bin/ruby', 'bin/jruby', 'bin/jruby*_*', 'bin/jruby*-*', '**/.*',
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
