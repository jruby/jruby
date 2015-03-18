require 'mspec/guards/guard'

class Object
  def env
    if PlatformGuard.windows?
      Hash[*`cmd.exe /C set`.split("\n").map { |e| e.split("=", 2) }.flatten]
    elsif PlatformGuard.opal?
      {}
    else
      Hash[*`env`.split("\n").map { |e| e.split("=", 2) }.flatten]
    end
  end

  def windows_env_echo(var)
    platform_is_not :opal do
      `cmd.exe /C ECHO %#{var}%`.strip
    end
  end

  def username
    if PlatformGuard.windows?
      windows_env_echo('USERNAME')
    elsif PlatformGuard.opal?
      ""
    else
      `whoami`.strip
    end
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
      name = ''
      platform_is_not :opal do
        name = `#{command}`
      end
      return name.strip if $?.success?
    end
    raise Exception, "hostname: unable to find a working command"
  end
end
