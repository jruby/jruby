# frozen_string_literal: true

MORE_QUIET = ENV['JRUBY_BUILD_MORE_QUIET']

if MORE_QUIET
  module Gem
    class Installer
      def say(message)
        return unless message != spec.post_install_message || !MORE_QUIET

        super
      end
    end
  end
end

def log(message = nil)
  puts message unless MORE_QUIET
end

default_gems = [
  # treat RGs update special:
  # - we do not want bin/update_rubygems or bin/gem overrides
  ['rubygems-update', '3.6.9', { bin: false, require_paths: ['lib'] }],
  ['bundler', '2.6.9'],
  ['cgi', '0.4.2'],
  # Currently using a stub gem for JRuby until we can incorporate our code.
  # https://github.com/ruby/date/issues/48
  ['date', '3.5.0'],
  ['delegate', '0.4.0'],
  ['did_you_mean', '2.0.0'],
  ['digest', '3.2.1'],
  ['english', '0.8.1'],
  # Ongoing discussion about the -java gem, since it just omits the ext: https://github.com/ruby/erb/issues/52
  ['erb', '5.1.3'],
  ['error_highlight', '0.7.0'],
  # https://github.com/ruby/etc/issues/19
  # ['etc', '1.4.6'],
  # https://github.com/ruby/fcntl/issues/9
  # ['fcntl', '1.3.0'],
  ['ffi', '1.17.0'],
  ['fileutils', '1.8.0'],
  ['find', '0.2.0'],
  ['forwardable', '1.3.3'],
  ['io-console', '0.8.1'],
  # https://github.com/ruby/io-nonblock/issues/4
  # ['io-nonblock', '0.3.2'],
  ['io-wait', '0.3.3'],
  ['ipaddr', '1.2.7'],
  ['jar-dependencies', '0.5.4'],
  ['jruby-readline', '1.3.7'],
  ['jruby-openssl', '0.15.4'],
  ['json', '2.15.2'],
  ['net-http', '0.7.0'],
  ['net-protocol', '0.2.2'],
  ['open-uri', '0.5.0'],
  ['open3', '0.2.1'],
  # https://github.com/ruby/openssl/issues/20#issuecomment-1022872855
  # ['openssl', '4.0.0.pre'],
  ['optparse', '0.8.0'],
  ['pp', '0.6.3'],
  ['prettyprint', '0.2.0'],
  # Not ready to ship in the box yet (native dependencies)
  # ['prism', '1.6.0'],
  ['psych', '5.2.6'],
  ['rake-ant', '1.0.6'],
  # https://github.com/ruby/resolv/issues/19
  # ['resolv', '0.6.3'],
  ['ruby2_keywords', '0.0.5'],
  ['securerandom', '0.4.1'],
  ['shellwords', '0.2.2'],
  ['singleton', '0.3.0'],
  ['stringio', '3.1.5'],
  ['strscan', '3.1.5'],
  ['subspawn', '0.1.1'], # has 3 transitive deps:
  ['subspawn-posix', '0.1.1'],
  ['ffi-binary-libfixposix', '0.5.1.1'],
  ['ffi-bindings-libfixposix', '0.5.1.0'],
  ['syntax_suggest', '2.0.2'],
  ['tempfile', '0.3.1'],
  ['time', '0.4.1'],
  ['timeout', '0.4.4'],
  # https://github.com/ruby/tmpdir/issues/13
  # ['tmpdir', '0.3.1'],
  ['tsort', '0.2.0'],
  ['un', '0.3.0'],
  ['uri', '1.1.1'],
  ['weakref', '0.1.4'],
  ['win32-registry', '0.1.1'],
  # https://github.com/ruby/win32ole/issues/12
  # ['win32ole', '1.9.0'],
  ['yaml', '0.4.0']
  # https://github.com/ruby/zlib/issues/38
  # ['zlib', '3.2.2'],
]

bundled_gems = [
  ['abbrev', '0.1.2'],
  ['base64', '0.3.0'],
  ['benchmark', '0.5.0'],
  # Extension still lives in JRuby. See https://github.com/ruby/bigdecimal/issues/268
  ['bigdecimal', '3.3.1'],
  ['csv', '3.3.5'],
  # Newer versions require deep control over CRuby internals, needs work to support JRuby.
  # ['debug', '1.11.0'],
  ['debug', '0.2.1'],
  ['drb', '2.2.3'],
  ['fiddle', '1.1.8'],
  ['getoptlong', '0.2.1'],
  ['irb', '1.15.3'],
  ['logger', '1.7.0'],
  ['matrix', '0.4.3'],
  ['minitest', '5.26.0'],
  ['mutex_m', '0.3.0'],
  ['net-ftp', '0.3.9'],
  ['net-imap', '0.5.12'],
  ['net-pop', '0.1.2'],
  ['net-smtp', '0.5.1'],
  ['nkf', '0.2.0'],
  ['observer', '0.1.2'],
  ['ostruct', '0.6.3'],
  ['power_assert', '3.0.0'],
  ['prime', '0.1.4'],
  ['pstore', '0.2.0'],
  ['racc', '1.8.1'],
  ['rake', '${rake.version}'],
  # Depends on many CRuby internals
  # ['rbs', '3.9.5'],
  ['rdoc', '6.15.1'],
  # Ext removed from CRuby in 3.3, equivalent for us would be to remove jruby-readline but unknown implications.
  # The gem below just attempts to load the extension, and failing that loads reline. Our current readline.rb in
  # jruby-readline does largely the same, but it finds the extension and does not load reline.
  # https://github.com/ruby/readline/issues/5
  # ['readline', '0.0.4'],
  # Will be solved with readline
  # ['readline-ext', '0.2.0'],
  ['reline', '0.6.2'],
  # Depends on prism gem with native ext
  # ['repl_type_completer', '0.1.12'],
  ['resolv-replace', '0.1.1'],
  ['rexml', '3.4.4'],
  ['rinda', '0.2.0'],
  ['rss', '0.3.1'],
  # https://github.com/ruby/syslog/issues/1
  # ['syslog', '0.3.0'],
  ['test-unit', '3.7.0']
  # Depends on many CRuby internals
  # ['typeprof', '0.30.1'],
]

project 'JRuby Lib Setup' do
  version = ENV['JRUBY_VERSION'] ||
            File.read(File.join(basedir, '..', 'VERSION')).strip

  model_version '4.0.0'
  id 'jruby-stdlib'
  inherit 'org.jruby:jruby-parent', version

  properties("polyglot.dump.pom": 'pom.xml',
             "polyglot.dump.readonly": true,
             "jruby.plugins.version": '3.0.6',
             "gem.home": '${basedir}/ruby/gems/shared',
             # we copy everything into the target/classes/META-INF
             # so the jar plugin just packs it - see build/resources below
             "jruby.complete.home": '${project.build.outputDirectory}/META-INF/jruby.home',
             "jruby.complete.gems": '${jruby.complete.home}/lib/ruby/gems/shared')

  # just depends on jruby-core so we are sure the jruby.jar is in place
  jar "org.jruby:jruby-core:#{version}", scope: 'test'

  extension 'org.jruby.maven:mavengem-wagon:2.0.2'

  repository id: :mavengems, url: 'mavengem:https://rubygems.org'

  # for testing out jruby-ossl before final release :
  # repository :id => 'gem-snaphots', :url => 'https://oss.sonatype.org/content/repositories/snapshots'
  # repository :id => 'gem-staging', :url => 'http://oss.sonatype.org/content/repositories/staging'

  plugin(:clean,
         filesets: [{ directory: '${basedir}/ruby/gems/shared/specifications/default',
                      includes: ['*'] },
                    { directory: '${basedir}/ruby/stdlib',
                      includes: ['org/**/*.jar'] }])

  # tell maven to download the respective gem artifacts
  default_gems.each do |name, version|
    # use provided scope so it is not a real dependency for runtime
    dependency 'rubygems', name, version, type: 'gem', scope: :provided do
      exclusion 'rubygems:jar-dependencies'
    end
  end

  bundled_gems.each do |name, version|
    # use provided scope so it is not a real dependency for runtime
    dependency 'rubygems', name, version, type: 'gem', scope: :provided do
      exclusion 'rubygems:jar-dependencies'
    end
  end

  default_gemnames = default_gems.collect(&:first)
  all_gems = default_gems + bundled_gems

  plugin :dependency,
         useRepositoryLayout: true,
         outputDirectory: 'ruby/stdlib',
         excludeGroupIds: 'rubygems',
         includeScope: :provided do
    execute_goal 'copy-dependencies', phase: 'generate-resources'
  end

  execute :install_gems, :initialize do |ctx|
    require 'fileutils'

    log "using jruby #{JRUBY_VERSION}"

    target = ctx.project.build.directory.to_pathname
    gem_home = File.join(target, 'rubygems')
    gems = File.join(gem_home, 'gems')
    specs = File.join(gem_home, 'specifications')
    cache = File.join(gem_home, 'cache')
    jruby_gems = File.join(ctx.project.basedir.to_pathname, 'ruby', 'gems', 'shared')
    bin_stubs = File.join(jruby_gems, 'gems')
    default_specs = File.join(jruby_gems, 'specifications', 'default')
    ruby_dir = File.join(ctx.project.basedir.to_pathname, 'ruby')
    stdlib_dir = File.join(ruby_dir, 'stdlib')
    jruby_home = ctx.project.parent.basedir.to_pathname

    FileUtils.mkdir_p(default_specs)

    # now we can require the rubygems staff
    require 'rubygems/installer'
    require 'rubygems/package'

    log 'install gems unless already installed'
    ENV_JAVA['jars.skip'] = 'true'

    # bin location for global binstubs
    global_bin = File.join(jruby_home, 'bin')

    # force Ruby command to "jruby" for the generated Windows bat files since we install using 9.1.17.0 jar file
    Gem.singleton_class.send(:define_method, :ruby) do
      File.join(global_bin, "jruby#{RbConfig::CONFIG['EXEEXT']}")
    end

    # Disable extension build for gems (none of ours require a build)
    # rubocop:disable Style/ClassAndModuleChildren
    class Gem::Ext::Builder
      def build_extensions
        return if @spec.extensions.empty?

        say 'Skipping native extensions.'

        FileUtils.mkdir_p File.dirname(@spec.gem_build_complete_path)
        FileUtils.touch @spec.gem_build_complete_path
      end
    end

    ctx.project.artifacts.select do |a|
      ['rubygems', 'org.jruby.gems'].include?(a.group_id)
    end.each do |a|
      ghome = default_gemnames.member?(a.artifact_id) ? gem_home : jruby_gems
      next unless Dir[File.join(ghome, 'cache', File.basename(a.file.to_pathname).sub(/.gem/, '*.gem'))].empty?

      log a.file.to_pathname
      installer = Gem::Installer.new(Gem::Package.new(a.file.to_pathname),
                                     wrappers: true,
                                     ignore_dependencies: true,
                                     install_dir: ghome,
                                     env_shebang: true)
      def installer.ensure_required_ruby_version_met; end
      installer.install
    end

    copy_gem_executables = lambda do |spec, gem_home|
      unless spec.executables.empty?
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
      version = version.sub(/-SNAPSHOT/, '')
      gem_name = "#{name}-#{version}"
      options = { bin: true, spec: true }.merge(options || {})

      # install the gem unless already installed
      next unless Dir[File.join(default_specs, "#{gem_name}*.gemspec")].empty?

      log
      log "--- gem #{gem_name} ---"

      # copy the gem content to stdlib

      log "copy gem content to #{stdlib_dir}"

      spec = Gem::Package.new(Dir[File.join(cache, "#{gem_name}*.gem")].first).spec

      require_paths = options[:require_paths] || spec.require_paths

      require_paths.each do |require_path|
        require_base = File.join(gems, "#{gem_name}*", require_path)
        require_files = File.join(require_base, '*')

        # copy in new ones and mark writable for future updates (e.g. minitest)
        stdlib_locs = Dir[require_files].map do |f|
          log " copying: #{f} to #{stdlib_dir}" # if $VERBOSE
          FileUtils.cp_r(f, stdlib_dir)

          stdlib_loc = f.sub(File.dirname(f), stdlib_dir)
          File.directory?(stdlib_loc) ? Dir["#{stdlib_loc}/*"].to_a : stdlib_loc
        end
        stdlib_locs.flatten!

        # fix permissions on copied files
        stdlib_locs.each do |f|
          next if File.writable? f

          log " fixing permissions: #{f}" if $VERBOSE
          # TODO: better way to just set it writable without changing all modes?
          FileUtils.chmod_R(0o644, f)
        end
      end

      # get gemspec
      specfile_wildcard = "#{gem_name}*.gemspec"
      specfile = Dir[File.join(specs, specfile_wildcard)].first

      unless specfile
        raise Errno::ENOENT,
              "gemspec #{specfile_wildcard} not found in #{specs}; dependency unspecified in lib/pom.xml?"
      end

      # copy bin files if the gem has any
      copy_gem_executables.call(spec, gem_home) if options[:bin]

      # TODO: try avoiding these binstub of gems - should use a full gem location
      spec.executables.each do |f|
        bin = Dir.glob(File.join(gems, "#{gem_name}*", spec.bindir))[0]
        source = File.join(bin, f)
        target = File.join(bin_stubs, source.sub(gems, ''))
        log "copy #{f} to #{target}"
        FileUtils.mkdir_p(File.dirname(target))
        FileUtils.cp_r(source, target)
      end

      next unless options[:spec]

      specname = File.basename(specfile)
      log "copy to specifications/default: #{specname}"
      File.open(File.join(default_specs, specname), 'w') do |f|
        f.print(spec.to_ruby)
      end
    end

    bundled_gems.each do |name, version| # copy bin files for bundled gems (e.g. rake) as well
      version = ctx.project.properties.get(version[2..-2]) || version # e.g. resolve '${rake.version}' from properties
      gem_name = "#{name}-#{version}"
      spec = Gem::Package.new(Dir[File.join(jruby_gems, 'cache', "#{gem_name}*.gem")].first).spec
      copy_gem_executables.call(spec, jruby_gems)
    end

    # patch jruby-openssl - remove file which should be only inside gem
    # use this instead of FileUtils.rm_f - issue #1698
    f = File.join(stdlib_dir, 'jruby-openssl.rb')
    File.delete(f) if File.exist?(f)

    # we do not want rubygems_plugin.rb within jruby
    f = File.join(stdlib_dir, 'rubygems_plugin.rb')
    File.delete(f) if File.exist?(f)

    # axiom-types appears to be a dead project but a transitive dep we still
    # have.  It contains unreadable files which messes up some upstream
    # maintainers like OpenBSD (see #1989).
    hack = File.join jruby_gems, 'gems', 'axiom-types-*'
    (Dir[File.join(hack, '**/*')] + Dir[File.join(hack, '**/.*')]).each do |f|
      next unless File.file?(f)

      begin
        FileUtils.chmod 'u+rw,go+r'
      rescue StandardError
        nil
      end
    end
  end

  execute('fix shebang on gem bin files and add *.bat files',
          'generate-resources') do |ctx|
    log 'generating missing .bat files'
    jruby_home = ctx.project.parent.basedir.to_pathname
    Dir[File.join(jruby_home, 'bin', '*')].each do |fn|
      next unless File.file?(fn)
      next if fn =~ /.bat$/
      next if File.exist?("#{fn}.bat")
      next unless File.open(fn, 'r', internal_encoding: 'ASCII-8BIT') do |io|
        line = begin
          io.readline
        rescue StandardError
          ''
        end
        line =~ /^#!.*ruby/
      end

      log " generating #{File.basename(fn)}.bat" if $VERBOSE
      File.open("#{fn}.bat", 'wb') do |f|
        f.print "@ECHO OFF\r\n"
        f.print "@\"%~dp0jruby.exe\" -S #{File.basename(fn)} %*\r\n"
      end
    end
  end

  execute 'jrubydir', 'prepare-package' do |ctx|
    require("#{ctx.project.basedir.to_pathname}/../core/src/main/ruby/jruby/commands.rb")
    JRuby::Commands.generate_dir_info("#{ctx.project.build.output_directory.to_pathname}/META-INF/jruby.home")
  end

  # we have no sources and attach an empty jar later in the build to
  # satisfy oss.sonatype.org upload

  plugin(:source, skipSource: 'true')

  # this plugin is configured to attach empty jars for sources and javadocs
  plugin('org.codehaus.mojo:build-helper-maven-plugin')

  build do
    resource do
      directory '${gem.home}'
      includes [
        'specifications/default/*',
        *all_gems.map { |name, version| "specifications/#{name}-#{version}*" },
        *all_gems.map { |name, version| "gems/#{name}-#{version}*/**/*" },
        *all_gems.map { |name, version| "cache/#{name}-#{version}*" }
      ]
      target_path '${jruby.complete.gems}'
    end

    resource do
      directory '${basedir}/..'
      includes 'bin/*', 'lib/ruby/include/**', 'lib/ruby/stdlib/**'
      excludes 'bin/jruby*_*', 'bin/jruby*-*', '**/.*',
               'lib/ruby/stdlib/rubygems/defaults/jruby_native.rb',
               'lib/ruby/stdlib/gauntlet*.rb' # gauntlet_rdoc.rb, gauntlet_rubygems.rb
      target_path '${jruby.complete.home}'
    end

    resource do
      directory '${project.basedir}/..'
      includes ['BSDL', 'COPYING', 'LEGAL', 'LICENSE.RUBY', 'VERSION']
      target_path '${project.build.outputDirectory}/META-INF/'
    end
  end
end
