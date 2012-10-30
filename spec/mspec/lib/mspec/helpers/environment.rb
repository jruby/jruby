require 'mspec/guards/guard'

class Object
  def env
    env = ""
    if PlatformGuard.windows?
      env = Hash[*`cmd.exe /C set`.split("\n").map { |e| e.split("=", 2) }.flatten]
    else
      env = Hash[*`env`.split("\n").map { |e| e.split("=", 2) }.flatten]
    end
    env
  end

  def windows_env_echo(var)
    `cmd.exe /C ECHO %#{var}%`.strip
  end

  def username
    user = ""
    if PlatformGuard.windows?
      user = windows_env_echo('USERNAME')
    else
      user = `whoami`.strip
    end
    user
  end

  def home_directory
    return ENV['HOME'] unless PlatformGuard.windows?
    windows_env_echo('HOMEDRIVE') + windows_env_echo('HOMEPATH')
  end

  def dev_null
    if PlatformGuard.windows?
      "NUL"
    else
      "/dev/null"
    end
  end

  def hostname
    commands = ['hostname', 'uname -n']
    commands.each do |command|
      name = `#{command}`
      return name.strip if $?.success?
    end
    raise Exception, "hostname: unable to find a working command"
  end
end
