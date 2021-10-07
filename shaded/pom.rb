version = ENV['JRUBY_VERSION'] ||
  File.read( File.join( basedir, '..', 'VERSION' ) ).strip

# note: the module name is logical and the artifact keeps it legacy name
project 'JRuby Core' do

  model_version '4.0.0'
  inherit 'org.jruby:jruby-parent', version
  id 'org.jruby:jruby-core'

  properties( 'polyglot.dump.pom' => 'pom.xml',
              'polyglot.dump.readonly' => true,
              'jruby.basedir' => '${basedir}/..'
            )

  jar 'org.jruby:jruby-base', '${project.version}'
  plugin :shade do
    execute_goals( 'shade',
                   id: 'create shaded jar',
                   phase: 'package',
                   artifactSet: {
                       excludes: 'javax.annotation:javax.annotation-api'
                   },
                   relocations: [
                       {pattern: 'org.objectweb', shadedPattern: 'org.jruby.org.objectweb' },
                       {pattern: 'me.qmx.jitescript', shadedPattern: 'org.jruby.me.qmx.jitescript'},
                   ],
                   transformers: [ {'@implementation' => 'org.apache.maven.plugins.shade.resource.ManifestResourceTransformer',
                                         mainClass: 'org.jruby.Main',
                                         manifestEntries: {'Automatic-Module-Name' => 'org.jruby.dist'}}],
                   createSourcesJar: true
    )
  end

  plugin :'com.coderplus.maven.plugins:copy-rename-maven-plugin' do
    execute_goals 'copy',
                  id: 'copy to lib/jruby.jar',
                  phase: 'package',
                  sourceFile: 'target/jruby-core-${project.version}.jar',
                  destinationFile: '${jruby.basedir}/lib/jruby.jar'
  end

  plugin( :compiler,
          'encoding' => 'utf-8',
          'verbose' => 'true',
          'fork' => 'false',
          'compilerArgs' => { 'arg' => '-J-Xmx1G' },
          'showWarnings' => 'true',
          'showDeprecation' => 'true',
          'source' => [ '${base.java.version}', '1.8' ],
          'target' => [ '${base.javac.version}', '1.8' ],
          'useIncrementalCompilation' =>  'false' )


  plugin( :source, 'skipSource' =>  'true' )

  [:all, :release, :main, :osgi, :j2ee, :complete, :dist, :'jruby_complete_jar_extended', :'jruby-jars' ].each do |name|
    profile name do
      # we shade in all dependencies which use the asm classes and relocate
      # the asm package-name. with all jruby artifacts behave the same
      # regarding asm: lib/jruby, jruby-core and jruby-complete via maven
      plugin :shade do
        execute_goals( 'shade',
                       id: 'shade dependencies into jar',
                       phase: 'package',
                       artifactSet: {
                           # IMPORTANT these needs to match exclusions in
                           # maven/jruby-complete/pom.rb
                           includes: [ 'com.github.jnr:jnr-ffi',
                                       'me.qmx.jitescript:jitescript',
                                       'org.ow2.asm:*'
                           ],
                           excludes: 'javax.annotation:javax.annotation-api'
                       },
                       relocations: [
                           {pattern: 'org.objectweb', shadedPattern: 'org.jruby.org.objectweb' },
                           {pattern: 'me.qmx.jitescript', shadedPattern: 'org.jruby.me.qmx.jitescript'},
                       ],
                       transformers: [ {'@implementation' => 'org.apache.maven.plugins.shade.resource.ManifestResourceTransformer',
                                         'mainClass' => 'org.jruby.Main',
                                         'manifestEntries' => {'Automatic-Module-Name' => 'org.jruby.dist'}}],
                       filters: [
                           {artifact: 'com.headius:invokebinder', excludes: '**/module-info.class'}
                       ],
                       createSourcesJar: true
        )
      end
    end
  end
end
