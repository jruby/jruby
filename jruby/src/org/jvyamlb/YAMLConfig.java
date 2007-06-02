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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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
package org.jvyamlb;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public interface YAMLConfig {
    YAMLConfig indent(final int indent);
    int indent();
    YAMLConfig useHeader(final boolean useHeader);
    boolean useHeader();
    YAMLConfig useVersion(final boolean useVersion);
    boolean useVersion();
    YAMLConfig version(final String version);
    String version();
    YAMLConfig explicitStart(final boolean expStart);
    boolean explicitStart();
    YAMLConfig explicitEnd(final boolean expEnd);
    boolean explicitEnd();
    YAMLConfig anchorFormat(final String format);
    String anchorFormat();
    YAMLConfig explicitTypes(final boolean expTypes);
    boolean explicitTypes();
    YAMLConfig canonical(final boolean canonical);
    boolean canonical();
    YAMLConfig bestWidth(final int bestWidth);
    int bestWidth();
    YAMLConfig useBlock(final boolean useBlock);
    boolean useBlock();
    YAMLConfig useFlow(final boolean useFlow);
    boolean useFlow();
    YAMLConfig usePlain(final boolean usePlain);
    boolean usePlain();
    YAMLConfig useSingle(final boolean useSingle);
    boolean useSingle();
    YAMLConfig useDouble(final boolean useDouble);
    boolean useDouble();
}// YAMLConfig
