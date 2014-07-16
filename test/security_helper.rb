class Wrapper
  java_import 'org.jruby.security.SecurityManager'

  attr_reader :java_manager

  def initialize(java_manager)
    @java_manager = java_manager
  end

  def allow(permissions_hash = nil, &block)
    if permissions_hash
      permissions = parse_hash_value(permissions_hash).uniq.map { |v| v.flatten }

      return allow do |expected_type, expected_name, expected_actions|
        permissions.any? do |parr|
          (type, name, actions) = *parr
          (type == expected_type &&
            (name.nil? || name == expected_name) &&
            (actions.nil? || actions == expected_actions))
        end
      end
    end

    SecurityManager::RubyPermission.new(block).tap { |p| java_manager.permit p }
  end

  def with_permissions(hash)
    p = allow(hash)
    begin
      yield
    ensure
      java_manager.revoke p
    end
  end

  def permissive!
    java_manager.setIsStrict(false)
    self
  end

  def strict!
    java_manager.setIsStrict(true)
    self
  end

  def verbose!
    java_manager.setVerbosity true
    self
  end

  def silent!
    java_manager.setVerbosity false
    self
  end

  private

  def parse_hash_value(value)
    if value.respond_to?(:each_pair)
      values = []
      value.each_pair { |k, v| values += [ k ].product(parse_hash_value(v)) }
      values
    else
      value.to_a.compact
    end
  end
end

::SecurityManager = Wrapper.new org.jruby.security.SecurityManager.install

SecurityManager.allow do |type, name, actions|
  case type
  when "FilePermission"
    # Allow to read anything within the jruby directory
    (name.start_with?(File.expand_path("../../", __FILE__)) && actions == "read") ||

    # Allow to load jdk classes
    name =~ /jdk/
  when 'LoggingPermission'
    # Allow any logging access
    false
  when 'PropertyPermission'
    (actions == "read") && [
      "java.protocol.handler.pkgs",
      "sun.misc.ProxyGenerator.saveGeneratedFiles",

      # FFI needs to be able to read its properties
      /^jnr\.ffi\..*/,

      # Allow reading any jruby properties
      /^jruby/,

      # Allow knowledge about environment
      /^os\..*/,
      /^user\..*/,
      "sun.arch.data.model",
      "java.home",

      # Allow proxies
      /^sun.reflect.proxy.*/
    ].any? { |read_permission| read_permission === name }
  when 'RuntimePermission'
    # Allow loading of native libraries
    (name =~ /^loadLibrary\..*\.so$/) ||
    (name == "loadLibrary.nio") ||

    # jnr.posix needs this
    (name == "accessDeclaredMembers") ||
    (name == "createClassLoader") ||

    # Let Main do System.exit
    (name == "exitVM.1") ||

    (name =~ /^accessClassInPackage\.sun.*$/) ||

    # Not sure what this is about
    (name == "getProtectionDomain") ||
    (name == "fileSystemProvider")
  when 'ReflectPermission'
    # JRuby makes heavy usage of reflection for dynamic invocation, etc
    name == "suppressAccessChecks"
  else
    false
  end
end

::SecurityManager.permissive!
