package org.jruby.security;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

public class SecurityManager extends java.lang.SecurityManager {
  public static class RubyPermission {
    private final IRubyObject lambda;

    public RubyPermission(IRubyObject lambda) {
      this.lambda = lambda;
    }

    public boolean matches(java.security.Permission perm) {
      Ruby runtime = lambda.getRuntime();

      return lambda.callMethod(runtime.getCurrentContext(),
          "call",
          new IRubyObject[] {
            RubyString.newString(runtime, perm.getClass().getSimpleName()),
            RubyString.newString(runtime, perm.getName()),
            RubyString.newString(runtime, perm.getActions())
      }).isTrue();
    }
  }

  public static SecurityManager install() {
    SecurityManager manager = new SecurityManager();

    System.setSecurityManager(manager);

    return manager;
  }

  private boolean verbose = false;
  private boolean strict = false;
  private final List<RubyPermission> temporaryPermissions = new ArrayList<RubyPermission>();

  @Override
  public void checkPermission(java.security.Permission perm) {
    for (RubyPermission permission: temporaryPermissions) {
      if (permission.matches(perm)) {
        return;
      }
    }

    if (strict) {
      logTrace(perm.toString() + " denied");
      super.checkPermission(perm);
    }
  }

  public SecurityManager setIsStrict(boolean strict) {
    this.strict = strict;
    return this;
  }

  public SecurityManager permit(RubyPermission permission) {
    temporaryPermissions.add(permission);
    return this;
  }

  public SecurityManager revoke(RubyPermission permission) {
    temporaryPermissions.remove(permission);
    return this;
  }

  public SecurityManager setVerbosity(boolean verbose) {
    this.verbose = verbose;
    return this;
  }

  private void logTrace(String message) {
    if (verbose) {
      new Exception(message).printStackTrace();
    }
  }
}
