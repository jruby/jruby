/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.JRubyFile;
import org.jruby.util.CommandlineParser;

public class RubyInstanceConfig {
    private InputStream input          = System.in;
    private PrintStream output         = System.out;
    private PrintStream error          = System.err;
    private Profile profile            = Profile.DEFAULT;
    private boolean objectSpaceEnabled = true;
    private String currentDirectory;
    private Map environment;
    
    public static interface LoadServiceCreator {
        LoadService create(Ruby runtime);

        LoadServiceCreator DEFAULT = new LoadServiceCreator() {
                public LoadService create(Ruby runtime) {
                    return new LoadService(runtime);
                }
            };
    }

    private LoadServiceCreator creator = LoadServiceCreator.DEFAULT;

    {
        if (Ruby.isSecurityRestricted())
            currentDirectory = "/";
        else {
            currentDirectory = JRubyFile.getFileProperty("user.dir");
            if (System.getProperty("jruby.objectspace.enabled") != null) {
                objectSpaceEnabled = Boolean.getBoolean("jruby.objectspace.enabled");
            }
        }
    }

    public LoadServiceCreator getLoadServiceCreator() {
        return creator;
    }

    public void setLoadServiceCreator(LoadServiceCreator creator) {
        this.creator = creator;
    }

    public LoadService createLoadService(Ruby runtime) {
        return this.creator.create(runtime);
    }

    public void updateWithCommandline(CommandlineParser cmdline) {
        this.objectSpaceEnabled = cmdline.isObjectSpaceEnabled();
    }

    public void setInput(InputStream newInput) {
        input = newInput;
    }

    public InputStream getInput() {
        return input;
    }

    public void setOutput(PrintStream newOutput) {
        output = newOutput;
    }

    public PrintStream getOutput() {
        return output;
    }

    public void setError(PrintStream newError) {
        error = newError;
    }

    public PrintStream getError() {
        return error;
    }

    public void setCurrentDirectory(String newCurrentDirectory) {
        currentDirectory = newCurrentDirectory;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setProfile(Profile newProfile) {
        profile = newProfile;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setObjectSpaceEnabled(boolean newObjectSpaceEnabled) {
        objectSpaceEnabled = newObjectSpaceEnabled;
    }

    public boolean isObjectSpaceEnabled() {
        return objectSpaceEnabled;
    }

    public void setEnvironment(Map newEnvironment) {
        environment = newEnvironment;
    }

    public Map getEnvironment() {
        return environment;
    }
}
