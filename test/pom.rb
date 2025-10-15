# frozen_string_literal: true

version = ENV['JRUBY_VERSION'] ||
          File.read(File.join(basedir, '..', 'VERSION')).strip

project 'JRuby Integration Tests' do
  model_version '4.0.0'

  inherit 'org.jruby:jruby-parent', version
  id 'org.jruby:jruby-tests'

  extension 'org.jruby.maven:mavengem-wagon:2.0.2'

  repository id: :mavengems, url: 'mavengem:http://rubygems.org'
  plugin_repository id: :mavengems, url: 'mavengem:http://rubygems.org'

  plugin_repository(url: 'https://central.sonatype.com/repository/maven-snapshots/',
                    id: 'sonatype') do
    releases 'false'
    snapshots 'true'
  end

  properties("polyglot.dump.pom": 'pom.xml',
             "jruby.home": '${basedir}/..',
             "gem.home": '${jruby.home}/lib/ruby/gems/shared')

  scope :test do
    jar 'junit:junit:4.11'
    jar 'commons-logging:commons-logging:1.1.3'
    jar 'org.livetribe:livetribe-jsr223:2.0.7'
    jar 'org.jruby:jruby-core', '${project.version}'
  end
  scope :provided do
    jar 'org.apache.ant:ant:${ant.version}'
  end
  jar('org.jruby:requireTest:1.0',
      scope: 'system',
      systemPath: '${project.basedir}/jruby/requireTest-1.0.jar')

  overrides do
    plugin('org.eclipse.m2e:lifecycle-mapping:1.0.0',
           lifecycleMappingMetadata: {
             pluginExecutions: [{ pluginExecutionFilter: {
                                    groupId: 'org.jruby.maven',
                                    artifactId: 'gem-maven-plugin',
                                    versionRange: '[1.0.0-rc3,)',
                                    goals: ['initialize']
                                  },
                                  action: {
                                    ignore: ''
                                  } }]
           })
  end

  jruby_plugin :gem, '${jruby.plugins.version}' do
    options = { phase: 'initialize',
                gemPath: '${gem.home}',
                gemHome: '${gem.home}',
                binDirectory: '${jruby.home}/bin',
                includeRubygemsInTestResources: 'false' }

    execute_goals('initialize', options)
  end

  plugin(:compiler,
         encoding: 'utf-8',
         debug: 'true',
         verbose: 'true',
         fork: 'true',
         showWarnings: 'true',
         showDeprecation: 'true',
         source: '${base.java.version}',
         target: '${base.java.version}')
  plugin :dependency do
    execute_goals('copy',
                  id: 'copy jars for testing',
                  phase: 'process-classes',
                  artifactItems: [{ groupId: 'junit',
                                    artifactId: 'junit',
                                    version: '4.11',
                                    type: 'jar',
                                    overWrite: 'false',
                                    outputDirectory: 'target',
                                    destFileName: 'junit.jar' },
                                  { groupId: 'com.googlecode.jarjar',
                                    artifactId: 'jarjar',
                                    version: '1.1',
                                    type: 'jar',
                                    overWrite: 'false',
                                    outputDirectory: 'target',
                                    destFileName: 'jarjar.jar' }])
  end

  plugin(:deploy,
         skip: 'true')
  plugin(:site,
         skip: 'true',
         skipDeploy: 'true')

  build do
    default_goal 'test'
    test_source_directory '.'
  end

  profile 'rake' do
    plugin :antrun do
      execute_goals('run',
                    id: 'rake',
                    phase: 'test',
                    configuration: [xml('<target><exec dir="${jruby.home}" executable="${jruby.home}/bin/jruby" failonerror="true"><env key="JRUBY_OPTS" value=""/><arg value="-S"/><arg value="rake"/><arg value="${task}"/></exec></target>')])
    end
  end

  profile 'jruby_complete_jar_extended' do
    jar 'org.jruby:jruby-complete', '${project.version}', scope: :provided

    plugin :antrun do
      %w[jruby objectspace slow].each do |index|
        filenames = []
        File.open(File.join(basedir, "#{index}.index")) do |file|
          file.each_line do |line|
            next if line =~ /^#/ || line.strip.empty?

            filename = "mri/#{line.chomp}"
            filename = "jruby/#{line.chomp}.rb" unless File.exist? File.join(basedir, filename)
            filename = "#{line.chomp}.rb" unless File.exist? File.join(basedir, filename)
            next if filename =~ %r{mri/psych/}
            next if filename =~ %r{mri/net/http/}
            next unless File.exist? File.join(basedir, filename)

            filenames << filename
          end
        end

        # some tests from jruby suite (test/jruby/test_kernel.rb, test/jruby/test_socket.rb) need sub-process control
        add_opens = if ENV_JAVA['java.specification.version'].to_i > 8
                      ['java.base/java.io=ALL-UNNAMED', 'java.base/sun.nio.ch=ALL-UNNAMED']
                    else
                      []
                    end

        execute_goals('run',
                      id: "jruby_complete_jar_#{index}",
                      phase: 'test',
                      configuration: [
                        xml("<target unless='maven.test.skip'>" \
                               "<exec dir='${jruby.home}' executable='java' failonerror='true'>" +
                                 add_opens.map { |value| "<arg value='--add-opens'/><arg value='#{value}'/>" }.join +
                                 "<arg value='-Djruby.home=${jruby.home}'/>" \
                                 "<arg value='-cp'/>" \
                                 "<arg value='core/target/test-classes:test/target/test-classes:maven/jruby-complete/target/jruby-complete-${project.version}.jar'/>" \
                                 "<arg value='-Djruby.home=${jruby.home}'/>" \
                                 "<arg value='-Djruby.aot.loadClasses=true'/>" \
                                 "<arg value='org.jruby.main.Main'/>" \
                                 "<arg value='-I.'/>" \
                                 "<arg value='-Itest'/>" \
                                 "<arg value='lib/ruby/gems/shared/gems/rake-${rake.version}/lib/rake/rake_test_loader.rb'/>" +
                                 filenames.map { |filename| "<arg value='test/#{filename}'/>" }.join +
                                 "<arg value='-v'/>
                                </exec>
                              </target>")
                      ])
      end
    end
  end
end
