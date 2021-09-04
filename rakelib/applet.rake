namespace :applet do
  file 'tool/jruby.keystore' do
    sh 'keytool -keystore tool/jruby.keystore -genkey -keyalg rsa -alias applet'
  end
  
  desc "Sign a copy of jruby-complete.jar for applet/javaws use."
  task :dist => 'tool/jruby.keystore' do
    cp "dist/jruby-complete-#{jruby_version}.jar", 'dist/jruby-complete-signed.jar'
    sh 'jarsigner -keystore tool/jruby.keystore dist/jruby-complete-signed.jar applet'
  end
end
