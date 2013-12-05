/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 * Copyright (C) 2002 Don Schwartz <schwardo@users.sourceforge.net>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.javasupport.test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RubyTestObject
{
    public RubyTestObject duplicate ();
    public boolean isSelf (RubyTestObject obj);

    public void noArgs ();

    public void setNumber (double d);
    public void setNumber (int i);
    public void setNumber (long l);
    public void setNumber (Double d);
    public void setNumber (Integer i);
    public void setNumber (Long l);

    public double getNumberAsDouble ();
    public int getNumberAsInt ();
    public long getNumberAsLong ();
    public Double getNumberAsDoubleObj ();
    public Integer getNumberAsIntObj ();
    public Long getNumberAsLongObj ();

    public String getString ();
    public void setString (String s);

    public boolean getBool ();
    public void setBool (boolean b);

    public Object getObject ();
    public void setObject (Object obj);

    public void setList (Collection l);
    public void setList (int[] l);
    public void setList (Object[] l);
    public List getList ();
    public Object[]  getListAsArray ();
    public String[]  getListAsStringArray ();
    public int[]     getListAsIntArray ();
    public Integer[] getListAsIntegerArray ();
    public Set       getListAsSet ();
    public Collection getListAsCollection ();

    public void setMap (Map m);
    public Map getMap ();

    public void addToList (Object obj);
    public void removeFromList (Object obj);
    public String joinList ();
}
