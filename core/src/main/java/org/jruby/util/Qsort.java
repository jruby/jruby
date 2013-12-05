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
 * Copyright (C) 2008-2009 Joseph LaFata <joe@quibb.org>
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
package org.jruby.util;

import java.util.ArrayList;
import java.util.Comparator;

public class Qsort {
private static final int SIZE_THRESHOLD = 16;

    public static void sort(Object[] a, Comparator c) {
        if(a.length < SIZE_THRESHOLD) {
            insertionsort(a, 0, a.length, c);
            return;
        }

        quicksort_loop(a, 0, a.length, c);
    }

    public static void sort(Object[] a, int begin, int end, Comparator c) {
        if (begin < end) {
            if((end - begin) < SIZE_THRESHOLD) {
                insertionsort(a, begin, end, c);
                return;
            }

            quicksort_loop(a, begin, end, c);
        }
    }

    private static void endTest(Object[] a, int lo, int hi, Comparator c) {
        if (c.compare(a[lo], a[lo + 1]) <= 0) {
            if (c.compare(a[hi - 2], a[hi - 1]) > 0) {
                bubbleUp(a, lo, hi-1, c);
            }
        } else {
            if (c.compare(a[hi - 2], a[hi - 1]) > 0) {
                insertionsort(a, lo, hi, c);
            } else {
                bubbleDown(a, lo, hi-1, c);
            }
        }

    }

    private static boolean seqtest(Object[] a, int lo, int hi, Comparator c) {
        for (int i = lo + 1; i < hi - 2; ++i) {
            if (c.compare(a[i], a[i + 1]) > 0) {
                return false;
            }
        }
        endTest(a, lo, hi, c);
        return true;
    }

    private static boolean revtest(Object[] a, int lo, int hi, Comparator c) {
        for (int i = lo + 1; i < hi - 2; ++i) {
            if (c.compare(a[i], a[i + 1]) < 0) {
                return false;
            }
        }

        // reverse the entire area of the array selected if it's reversed.
        int i = lo;
        int j = hi - 1;
        while (i < j) {
            swap(a, i++, j--);
        }
        endTest(a, lo, hi, c);
        return true;
    }



    private static void quicksort_loop(Object[] a, int lo, int hi, Comparator c) {
        final ArrayList<int[]> stack = new ArrayList<int[]>(16);

        int[] entry = new int[2];
        entry[0] = lo;
        entry[1] = hi;

        while (!stack.isEmpty() || entry != null) {

            if (entry == null) {
                entry = stack.remove(stack.size() - 1);
            }
            lo = entry[0];
            hi = entry[1];

            int midi = lo + (hi - lo) / 2;
            Object mid = a[midi];
            Object m1;
            Object m3;

            // do median of 7 if the array is over 200 elements.
            if ((hi - lo) >= 200) {
                int t = (hi - lo) / 8;
                m1 = med3(a[lo + t], a[lo + t * 2], a[lo + t * 3], c);
                m3 = med3(a[midi + t], a[midi + t * 2], a[midi + t * 3], c);
            } else {
                // if it's less than 200 do median of 3
                int t = (hi - lo) / 4;
                m1 = a[lo + t];
                m3 = a[midi + t];
            }
            mid = med3(m1, mid, m3, c);

            if (hi - lo >= 63) {
                if (c.compare(m1, mid) <= 0 && c.compare(mid, m3) <= 0) {
                    if (seqtest(a, lo, hi, c)) {
                        entry = null;
                        continue;
                    }
                }
                else if (c.compare(m1, mid) >= 0 && c.compare(mid, m3) >= 0) {
                    if (revtest(a, lo, hi, c)) {
                        entry = null;
                        continue;
                    }
                }
            }

            int[] p = partition(a, lo, hi, mid, c);

            if(hi - p[1] > SIZE_THRESHOLD && p[0] - lo > SIZE_THRESHOLD) {
                entry[0] = p[1];
                entry[1] = hi;
                stack.add(entry);

                entry = new int[2];
                entry[0] = lo;
                entry[1] = p[0];
            } else if (hi - p[1] > SIZE_THRESHOLD) {
                entry[0] = p[1];
                entry[1] = hi;
                insertionsort(a, lo, p[0], c);
            } else if (p[0] - lo > SIZE_THRESHOLD) {
                entry[0] = lo;
                entry[1] = p[0];
                insertionsort(a, p[1], hi, c);
            } else {
                insertionsort(a, lo, p[0], c);
                insertionsort(a, p[1], hi, c);
                entry = null;
            }
        }
    }


    private static int[] partition(Object[] a, int lo1, int hi, Object x, Comparator comp) {
        int lo = lo1;
        int i = lo, j = hi, c = 0;

        while (true) {
            while (i < j && (c = comp.compare(a[i], x)) <= 0) {
                if (c == 0) {
                    if (i > lo) {
                        swap(a, lo++, i);
                    } else {
                        lo++;
                    }
                }
                i++;
            }
            j--;

            while (j >= i && (c = comp.compare(x, a[j])) < 0) {
                j--;
            }

            if (i > j) {
                break;
            }

            if (c == 0) {

                swap(a, i, j);

                if (i > lo) {
                    swap(a, lo++, i);
                } else {
                    lo++;
                }

            } else {
                swap(a, i, j);
            }
            i++;
        }

        c = ((i >= hi) ? hi-1 : i);

        while (c > lo1 && comp.compare(x, a[c]) < 0 ) {
            c--;
        }

        lo--;

        while (lo >= lo1 && c > lo)
            swap(a, lo1++, c--);

        return new int[]{(c > lo) ? c + 1 : lo1, i};
    }



    private static Object med3(Object lo, Object mid, Object hi, Comparator c) {
        if (c.compare(mid, lo) < 0) {
            if (c.compare(hi, mid) < 0) {
                return mid;
            } else {
                if (c.compare(hi, lo) < 0) {
                    return hi;
                } else {
                    return lo;
                }
            }
        } else {
            if (c.compare(hi, mid) < 0) {
                if (c.compare(hi, lo) < 0) {
                    return lo;
                } else {
                    return hi;
                }
            } else {
                return mid;
            }
        }
    }

    private static void insertionsort(Object[] a, int lo, int hi, Comparator c) {
        for (int i = lo+1; i < hi; i++) {
            int j = i;
            Object t = a[j];
            while (j > lo && c.compare(t, a[j - 1]) < 0) {
                a[j] = a[j - 1];
                --j;
            }
            a[j] = t;
        }
    }

    private static void bubbleDown(Object[] a, int lo, int hi, Comparator c) {
        Object x = a[lo];
        while (lo < hi && c.compare(x, a[lo+1]) > 0) {
            a[lo] = a[++lo];
        }
        a[lo] = x;
    }

    private static void bubbleUp(Object[] a, int lo, int hi, Comparator c) {
        Object x = a[hi];
        while (hi > lo && c.compare(x, a[hi-1]) < 0) {
            a[hi] = a[--hi];
        }
        a[hi] = x;
    }

    private static void swap(Object[] a, int i, int j) {
        Object t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}

