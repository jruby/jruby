require 'bundler'
Bundler::GemHelper.install_tasks

task :compile do
  require 'ant'
  ant ['-f', 'build.xml']
  cp 'dist/win32ole.jar', 'lib/win32ole/win32ole.jar'
end
