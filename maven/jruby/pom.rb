# frozen_string_literal: true

project 'JRuby Main Maven Artifact' do
  version = ENV['JRUBY_VERSION'] ||
            File.read(File.join(basedir, '..', '..', 'VERSION')).strip

  model_version '4.0.0'
  id "org.jruby:jruby:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"

  # keep it a jar even without sources - easier to add to a project
  packaging 'jar'

  jar 'org.jruby:jruby-base:${project.version}'
  jar 'org.jruby:jruby-stdlib:${project.version}'

  # we have no sources and attach an empty jar later in the build to
  # satisfy oss.sonatype.org upload
  plugin(:source, skipSource: 'true')

  # this plugin is configured to attach empty jars for sources and javadocs
  plugin('org.codehaus.mojo:build-helper-maven-plugin')

  plugin(:invoker, properties: { localRepository: '${settings.localRepository}' })

  build do
    resource do
      directory '${project.basedir}/../..'
      includes ['BSDL', 'COPYING', 'LEGAL', 'LICENSE.RUBY', 'VERSION']
      target_path '${project.build.outputDirectory}/META-INF/'
    end
  end

  plugin('net.ju-n.maven.plugins:checksum-maven-plugin')

  profile :apps do
    activation do
      property name: 'invoker.test'
    end

    properties "invoker.skip": false, "invoker.test": 'hellowarld_*'

    execute 'setup web applications', phase: 'pre-integration-test' do |ctx|
      def setup(ctx, framework, package, type, server)
        puts [framework, package, server].join "\t"

        source = File.join(ctx.basedir.to_pathname, 'src', 'templates', 'hellowarld')
        target = File.join(ctx.basedir.to_pathname, "src/it/hellowarld_#{server}_#{package}_#{framework}")

        FileUtils.rm_rf(target)
        FileUtils.cp_r(source, target)
        FileUtils.mv("#{target}/config-#{framework}.ru", "#{target}/config.ru")
        FileUtils.mv("#{target}/app-#{framework}", "#{target}/app")
        file = "#{target}/Gemfile-#{framework}"
        FileUtils.mv(file, "#{target}/Gemfile") if File.exist?(file)
        file = "#{target}/Gemfile-#{framework}.lock"
        FileUtils.mv(file, "#{target}/Gemfile.lock") if File.exist?(file)
        file = File.join(target, 'Mavenfile')
        File.write(file, File.read(file).sub(/pom/, type))
        File.open(File.join(target, 'invoker.properties'), 'w') do |f|
          f.puts "invoker.profiles = #{package},#{server},#{framework}"
        end
      end

      %w[cuba sinatra rails4].each do |framework|
        [%w[filesystem pom], %w[runnable jrubyJar]].each do |package|
          %w[webrick puma torquebox].each do |server|
            setup(ctx, framework, *package, server)
          end
        end
        [%w[warfile jrubyWar]].each do |package|
          %w[jetty tomcat wildfly_unpacked wildfly_packed].each do |server|
            # ', 'websphere' ].each do |server|
            # rails4 on wildfly complains about missing com.sun.org.apache.xpath.internal.VariableStack which are actually xalan classes used by nokogiri
            setup(ctx, framework, *package, server) unless (framework == 'rails4') && server =~ /wildfly/
          end
        end
      end
    end
  end

  profile :osgi do
    activation do
      property name: 'invoker.test'
    end

    execute 'setup osgi integration tests', phase: 'pre-integration-test' do |ctx|
      source = File.join(ctx.basedir.to_pathname, 'src', 'templates', 'osgi_all_inclusive')
      ['knoplerfish', 'equinox-3.6', 'equinox-3.7', 'felix-3.2', 'felix-4.4'].each do |m|
        target = File.join(ctx.basedir.to_pathname, 'src', 'it', "osgi_all_inclusive_#{m}")
        FileUtils.rm_rf(target)
        FileUtils.cp_r(source, target)
        File.open(File.join(target, 'invoker.properties'), 'w') do |f|
          f.puts "invoker.profiles = #{m}"
        end
      end
    end
  end

  plugin(:clean) do
    execute_goals(:clean,
                  phase: :clean,
                  id: 'clean-extra-osgi-ITs',
                  filesets: [{ directory: '${basedir}/src/it',
                               includes: ['osgi*/**'] }],
                  failOnError: false)
  end

  profile id: :jdk8 do
    activation do
      jdk '17'
    end
    plugin :invoker,
           pomExcludes: ['extended/pom.xml', 'osgi_all_inclusive_felix-3.2/pom.xml', '${its.j2ee}', '${its.osgi}']
  end

  profile id: :wlp do
    activation do
      property name: 'wlp.jar'
    end
    execute :install_wlp, phase: :'pre-integration-test' do |ctx|
      wlp = ctx.project.properties['wlp.jar'] || java.lang.System.properties['wlp.jar']
      system("java -jar #{wlp.to_pathname} --acceptLicense #{ctx.project.build.directory.to_pathname}")
      system(File.join(ctx.project.build.directory.to_pathname,
                       'wlp/bin/server') + 'create testing')
      FileUtils.cp_r(File.join(ctx.basedir.to_pathname, 'src/templates/j2ee_wlp'),
                     File.join(ctx.basedir.to_pathname, 'src/it'))
    end
  end
end
