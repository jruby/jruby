/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
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
package org.jruby.runtime.profile;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import org.jruby.util.collections.IntHashMap;

public class HtmlProfilePrinter extends ProfilePrinter {
  private static final long LIMIT = 100000000;
    
  public HtmlProfilePrinter(ProfileData profileData) {
      super(profileData);
  }
    
  HtmlProfilePrinter(ProfileData profileData, Invocation topInvocation) {
      super(profileData, topInvocation);
  }

  public void printHeader(PrintStream out) {
    out.println(head);
    out.println("<body>");
  }

  public void printFooter(PrintStream out) {
    out.println("</body>");
    out.println("</html>");
  }

  @Override
  public void printProfile(PrintStream out, boolean first) {
    final Invocation topInvocation = getTopInvocation();
    
    out.printf("<h1>Profile Report: %s</h1>\n", getThreadName());
    out.println("<h3>Total time: " + nanoString(topInvocation.getDuration()) + "</h3>");

    out.println("<table>\n" +
        "  <tr>\n" +
        "    <th> %Total</th>\n" +
        "    <th> %Self</th>\n" +
        "    <th> Total</th>\n" +
        "    <th> Self</th>\n" +
        "    <th> Children</th>\n" +
        "    <th> Calls</th>\n" +
        "    <th>Name</th>\n" +
        "  </tr>");


    IntHashMap<MethodData> methods = methodData(topInvocation);
    MethodData[] sortedMethods = methods.values().toArray(new MethodData[methods.size()]);

    Arrays.sort(sortedMethods, new Comparator<MethodData>() {
      public int compare(MethodData md1, MethodData md2) {
        long time1 = md1.totalTime();
        long time2 = md2.totalTime();
        return time1 == time2 ? 0 : (time1 < time2 ? 1 : -1);
      }
    });

    for (final MethodData data : sortedMethods) {
      if (!isProfilerInvocation(data.invocations.get(0))) {
        out.println("<tr class='break'><td colspan='7'></td></tr>");
        int serial = data.serialNumber;

        if (serial != 0) {
          Integer[] parentSerials = parentSerials(data);

          if (parentSerials.length > 0) {
            for (int parentSerial : parentSerials) {
              printInvocationFromParent(out, data, parentSerial, methodName(parentSerial), data.rootInvocationsFromParent(parentSerial));
            }
          }
        }

        String displayName = methodName(serial);
        if (data.totalTime() >= LIMIT) {
          out.println("<tr class='method'>");
          if (topInvocation.getDuration() == 0) {
            out.println("  <td>100%</td>");
            out.println("  <td>100%</td>");
          } else {
            out.println("  <td>" + Long.toString(data.totalTime() * 100 / topInvocation.getDuration()) + "%</td>");
            out.println("  <td>" + Long.toString(data.selfTime() * 100 / topInvocation.getDuration()) + "%</td>");
          }
          printTimingCells(out, data);
          out.println("  <td>" + Integer.toString(data.totalCalls()) + "</td>");
          out.println("  <td>" + methodAnchor(displayName) + "</td>");
          out.println("</tr>");
        }

        int[] childSerialsInts = data.children();
        Integer[] childSerials = new Integer[childSerialsInts.length];
        for (int i = 0; i < childSerialsInts.length; i++) {
          childSerials[i] = childSerialsInts[i];
        }

        Arrays.sort(childSerials, new Comparator<Integer>() {
          public int compare(Integer child1, Integer child2) {
            long time1 = data.rootInvocationsOfChild(child1).totalTime();
            long time2 = data.rootInvocationsOfChild(child2).totalTime();
            return time1 == time2 ? 0 : (time1 < time2 ? 1 : -1);
          }
        });

        if (childSerials.length > 0) {
          for (int childSerial : childSerials) {
            if (!isThisProfilerInvocation(childSerial)) {
              String callerName = methodName(childSerial);
              InvocationSet invs = data.rootInvocationsOfChild(childSerial);
              printInvocationOfChild(out, methods, data, childSerial, callerName, invs);
            }
          }
        }
      }
    }
    out.println("</table>");
  }

  private void printInvocationOfChild(PrintStream out, IntHashMap<MethodData> methods, MethodData data, int childSerial, String callerName, InvocationSet invs) {
    out.print("<!-- " + invs.totalTime() + " -->");
    if (invs.totalTime() < LIMIT) {
      return;
    }
    out.println("<tr>");
    out.println("  <td></td>");
    out.println("  <td></td>");
    printTimingCells(out, invs);
    out.println("  <td>" + Integer.toString(data.invocationsOfChild(childSerial).totalCalls()) + "/" + Integer.toString(methods.get(childSerial).totalCalls()) + "</td>");
    out.println("  <td>" + linkToMethod(callerName) + "</td>");
    out.println("</tr>");
  }

  private void printInvocationFromParent(PrintStream out, MethodData data, int parentSerial, String callerName, InvocationSet invs) {
    if (invs.totalTime() < LIMIT) {
      return;
    }
    out.println("<tr>");
    out.println("  <td></td>");
    out.println("  <td></td>");
    printTimingCells(out, invs);
    out.println("  <td>" + Integer.toString(data.invocationsFromParent(parentSerial).totalCalls()) + "/" + Integer.toString(data.totalCalls()) + "</td>");
    out.println("  <td>" + linkToMethod(callerName) + "</td>");
    out.println("</tr>");
  }

  private String linkToMethod(String callerName) {
    return "<a href='#" + callerName.replaceAll("[><#\\.\\?=:]", "_") + "'>" + callerName + "</a>";
  }

  private String methodAnchor(String callerName) {
    return "<a name='" + callerName.replaceAll("[><#\\.\\?=:]", "_") + "'>" + callerName + "</a>";
  }

  private void printTimingCells(PrintStream out, InvocationSet invs) {
    out.println("  <td>" + nanoString(invs.totalTime()) + "</td>");
    out.println("  <td>" + nanoString(invs.selfTime()) + "</td>");
    out.println("  <td>" + nanoString(invs.childTime()) + "</td>");
  }

  private Integer[] parentSerials(final MethodData data) {
    int[] parentSerialsInts = data.parents();
    Integer[] parentSerials = new Integer[parentSerialsInts.length];
    for (int i = 0; i < parentSerialsInts.length; i++) {
      parentSerials[i] = parentSerialsInts[i];
    }

    Arrays.sort(parentSerials, new Comparator<Integer>() {
      public int compare(Integer parent1, Integer parent2) {
        long time1 = data.rootInvocationsFromParent(parent1).totalTime();
        long time2 = data.rootInvocationsFromParent(parent2).totalTime();
        return time1 == time2 ? 0 : (time1 < time2 ? -1 : 1);
      }
    });
    return parentSerials;
  }

  String head = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n" +
      "<html>\n" +
      "<head>\n" +
      "  <style media=\"all\" type=\"text/css\">\n" +
      "    table {\n" +
      "      border-collapse: collapse;\n" +
      "      border: 1px solid #CCC;\n" +
      "      font-family: Verdana, Arial, Helvetica, sans-serif;\n" +
      "      font-size: 9pt;\n" +
      "      line-height: normal;\n" +
      "      width: 100%;\n" +
      "    }\n" +
      "\n" +
      "    th {\n" +
      "      text-align: center;\n" +
      "      border-top: 1px solid #FB7A31;\n" +
      "      border-bottom: 1px solid #FB7A31;\n" +
      "      background: #FFC;\n" +
      "      padding: 0.3em;\n" +
      "      border-left: 1px solid silver;\n" +
      "    }\n" +
      "\n" +
      "    tr.break td {\n" +
      "      border: 0;\n" +
      "      border-top: 1px solid #FB7A31;\n" +
      "      padding: 0;\n" +
      "      margin: 0;\n" +
      "    }\n" +
      "\n" +
      "    tr.method td {\n" +
      "      font-weight: bold;\n" +
      "    }\n" +
      "\n" +
      "    td {\n" +
      "      padding: 0.3em;\n" +
      "    }\n" +
      "\n" +
      "    td:first-child {\n" +
      "      width: 190px;\n" +
      "      }\n" +
      "\n" +
      "    td {\n" +
      "      border-left: 1px solid #CCC;\n" +
      "      text-align: center;\n" +
      "    }\n" +
      "\n" +
      "    .method_name {\n" +
      "      text-align: left;\n" +
      "    }\n" +
      "  </style>\n" +
      "  </head>\n";

}
