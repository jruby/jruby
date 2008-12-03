package org.jruby.util;

import java.util.Stack;
import java.util.Comparator;

/**
 * 
 * @author Joseph LaFata (qbproger)
 */
public class Qsort {
    /*
     * Class Variables
     */

    private static int SIZE_THRESHOLD = 16;
    private static int[] INCREMENT = new int[]{1, 4, 10, 23, 57,
        132, 301, 701, 1750, 4023, 9258, 21293, 48974, 112640
    };

    /**
     * The public function for sorting an array that's Comparable.
     * @param a
     */
    public static void sort(Comparable[] a) {
        if(a.length < SIZE_THRESHOLD) {
            insertionsort(a, 0, a.length);
            return;
        }
            
        if(!quicksort_loop(a, 0, a.length, 2 * floorLog2(a.length))) {
            insertionsort(a, 0, a.length);
        }
    }
    
    public static void sort(Object[] a, Comparator c) {
        if(a.length < SIZE_THRESHOLD) {
            insertionsort(a, 0, a.length, c);
            return;
        }
    
        if(!quicksort_loop(a, 0, a.length, 2 * floorLog2(a.length), c)) {
            insertionsort(a, 0, a.length, c);
        }
    }
    

    /**
     * Sort the items in the array passed in, in ascending order.
     * @param a - the array
     * @param lo - Starting element index inclusive
     * @param hi - End element index exclusive
     */
    public static void sort(Comparable[] a, int begin, int end) {
        if (begin < end) {
            if((end - begin) < SIZE_THRESHOLD) {
                insertionsort(a, begin, end);
                return;
            }
            
            if(!quicksort_loop(a, begin, end, 2 * floorLog2(end - begin)))
                insertionsort(a, begin, end);
        }
    }
    
    public static void sort(Object[] a, int begin, int end, Comparator c) {
        if (begin < end) {
            if((end - begin) < SIZE_THRESHOLD) {
                insertionsort(a, begin, end, c);
                return;
            }
            
            if(!quicksort_loop(a, begin, end, 2 * floorLog2(end - begin), c))
                insertionsort(a, begin, end, c);
        }
    }

    /**
     * This function is used to test the ends of the array.  If the array
     * is ordered, but the last element is out of order, this function catches
     * that case.
     * @param a - the array
     * @param lo - Starting element index inclusive
     * @param hi - End element index exclusive
     * @return
     */
    private static void endTest(Comparable[] a, int lo, int hi) {
        int comp1 = a[lo].compareTo(a[lo + 1]);
        int comp2 = a[hi - 2].compareTo(a[hi - 1]);


        if (comp1 <= 0) {
            if (comp2 > 0) {
                bubbleUp(a, lo, hi);
            }
        } else {
            if (comp2 <= 0) {
                bubbleDown(a, lo, hi);
            } else {
                bubbleBoth(a, lo, hi);
            }
        }
        
    }
    
    private static void endTest(Object[] a, int lo, int hi, Comparator c) {
        int comp1 = c.compare(a[lo], a[lo + 1]);
        int comp2 = c.compare(a[hi - 2], a[hi - 1]);


        if (comp1 <= 0) {
            if (comp2 > 0) {
                bubbleUp(a, lo, hi, c);
            }
        } else {
            if (comp2 <= 0) {
                bubbleDown(a, lo, hi, c);
            } else {
                bubbleBoth(a, lo, hi, c);
            }
        }
        
    }

    /**
     * Test to see if the array is in sequential order.  This array will test
     * the elements from lo+1 to hi-2 leaving out the ends because those
     * are tested separately.
     * @param a - the array
     * @param lo - Starting element index inclusive
     * @param hi - End element index exclusive
     * @return true if the array is sorted for the given indices.
     */
    private static boolean seqtest(Comparable[] a, int lo, int hi) {

        for (int i = lo + 1; i < hi - 2; ++i) {
            if (a[i].compareTo(a[i + 1]) > 0) {
                return false;
            }
        }
        endTest(a, lo, hi);
        return true;
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

    /**
     * Test to see if the array is in reverse order.  This array will test
     * the elements from lo+1 to hi-2 leaving out the ends because those
     * are tested separately.
     * 
     * This function will reverse the array if it's found
     * to be in reverse order.
     * 
     * @param a - the array
     * @param lo - Starting element index inclusive
     * @param hi - End element index exclusive
     * @return true if the array is sorted for the given indices.
     */
    private static boolean revtest(Comparable[] a, int lo, int hi) {
        for (int i = lo + 1; i < hi - 2; ++i) {
            if (a[i].compareTo(a[i + 1]) < 0) {
                return false;
            }
        }
        // reverse the entire area of the array selected if it's reversed.
        int i = lo;
        int j = hi - 1;
        while (i < j) {
            swap(a, i, j);
            i++;
            j--;
        }
        endTest(a, lo, hi);
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
            swap(a, i, j);
            i++;
            j--;
        }
        endTest(a, lo, hi, c);
        return true;
    }

    /**
     * The quicksort loop of the sorting function.  It uses a stack and does 
     * the quicksort iteratively.
     * 
     * @param a - the array
     * @param lo - Starting element index inclusive
     * @param hi - End element index exclusive
     * @param depth_limit1 the depth limit after which the algorithm changes
     *                      strategy.
     */
    private static boolean quicksort_loop(Comparable[] a, int lo1, int hi1, int depth_limit1) {
        boolean done = false;
        Stack<int[]> stack = new Stack<int[]>();

        int[] entry = new int[3];
        entry[0] = lo1;
        entry[1] = hi1;
        entry[2] = depth_limit1;
        stack.push(entry);

        boolean checklim = true;

        while (stack.size() > 0) {
            entry = stack.pop();
            int lo = entry[0];
            int hi = entry[1];
            int depth_limit = entry[2];

            // if the depth hits 0 switch to shell sort
            // and continue.
            if (depth_limit == 0) {
                shellsort(a, lo, hi);
                continue;
            }

            depth_limit--;

            int midi = lo + (hi - lo) / 2;
            Comparable mid = a[midi];
            Comparable m1;
            Comparable m3;

            // do median of 7 if the array is over 200 elements.
            if ((hi - lo) >= 200) {
                int t = (hi - lo) / 8;
                m1 = med3(a[lo + t], a[lo + t * 2], a[lo + t * 3]);
                m3 = med3(a[midi + t], a[midi + t * 2], a[midi + t * 3]);
            } else {
                // if it's less than 200 do median of 3
                int t = (hi - lo) / 4;
                m1 = a[lo + t];
                m3 = a[midi + t];
            }
            mid = med3(m1, mid, m3);

            // if checklim is true and the length is greater than
            // 63.  63 seemed arbirary to me, but that's the number
            // ruby was using.  Only run this check once.  This is designed
            // to catch sorted/reversed arrays.
            if (checklim && hi - lo >= 63) {
                checklim = false;
                int comp1 = m1.compareTo(mid);
                int comp2 = mid.compareTo(m3);
                if (comp1 <= 0 && comp2 <= 0) {
                    if (seqtest(a, lo, hi)) {
                        if(lo == lo1 && hi == hi1)
                            done = true;
                        continue;
                    }
                }

                if (comp1 >= 0 && comp2 >= 0) {
                    if (revtest(a, lo, hi)) {
                        if(lo == lo1 && hi == hi1)
                            done = true;
                        continue;
                    }
                }
            }

            int p = partition(a, lo, hi, mid);

            // If it's greater than the threshold push it on the stack.
            boolean entryUsed = false;
            if (hi - p > SIZE_THRESHOLD) {
                entryUsed = true;
                entry[0] = p;
                entry[1] = hi;
                entry[2] = depth_limit;
                stack.push(entry);
            }

            // If it's greater than the threshold push it on the stack.
            if (p - lo > SIZE_THRESHOLD) {
                if(entryUsed) entry = new int[3];
                entry[0] = lo;
                entry[1] = p;
                entry[2] = depth_limit;
                stack.push(entry);
            }
        }
        
        return done;
    }
    
    private static boolean quicksort_loop(Object[] a, int lo1, int hi1, int depth_limit1, Comparator c) {
        boolean done = false;
        Stack<int[]> stack = new Stack<int[]>();

        int[] entry = new int[3];
        entry[0] = lo1;
        entry[1] = hi1;
        entry[2] = depth_limit1;
        stack.push(entry);

        boolean checklim = true;

        while (stack.size() > 0) {
            entry = stack.pop();
            int lo = entry[0];
            int hi = entry[1];
            int depth_limit = entry[2];

            // if the depth hits 0 switch to shell sort
            // and continue.
            if (depth_limit == 0) {
                shellsort(a, lo, hi, c);
                continue;
            }

            depth_limit--;

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

            // if checklim is true and the length is greater than
            // 63.  63 seemed arbirary to me, but that's the number
            // ruby was using.  Only run this check once.  This is designed
            // to catch sorted/reversed arrays.
            if (checklim && hi - lo >= 63) {
                checklim = false;
                int comp1 = c.compare(m1, mid);
                int comp2 = c.compare(mid, m3);
                if (comp1 <= 0 && comp2 <= 0) {
                    if (seqtest(a, lo, hi, c)) {
                        if(lo == lo1 && hi == hi1)
                            done = true;
                        continue;
                    }
                }

                if (comp1 >= 0 && comp2 >= 0) {
                    if (revtest(a, lo, hi, c)) {
                        if(lo == lo1 && hi == hi1)
                            done = true;
                        continue;
                    }
                }
            }

            int p = partition(a, lo, hi, mid, c);

            // If it's greater than the threshold push it on the stack.
            boolean entryUsed = false;
            if (hi - p > SIZE_THRESHOLD) {
                entryUsed = true;
                entry[0] = p;
                entry[1] = hi;
                entry[2] = depth_limit;
                stack.push(entry);
            }

            // If it's greater than the threshold push it on the stack.
            if (p - lo > SIZE_THRESHOLD) {
                if(entryUsed) entry = new int[3];
                entry[0] = lo;
                entry[1] = p;
                entry[2] = depth_limit;
                stack.push(entry);
            }
        }
        
        return done;
    }

    /**
     * Partition the array between the two indices into two groups based on
     * the Comparable passed in x.
     * @param a - the array
     * @param lo - Starting element index inclusive
     * @param hi - End element index exclusive
     * @param x - The comparable to use to parition the elements.
     * @return The pivot index
     */
    private static int partition(Comparable[] a, int lo, int hi, Comparable x) {
        int i = lo, j = hi;
        while (true) {
            while (i < hi && a[i].compareTo(x) < 0) {
                i++;
            }
            j--;
            while (j >= lo && x.compareTo(a[j]) < 0) {
                j--;
            }
            if (!(i < j)) {
                return i;
            }
            swap(a, i, j);
            i++;
        }
    }

    private static int partition(Object[] a, int lo, int hi, Object x, Comparator c) {
        int i = lo, j = hi;
        while (true) {
            while (i < hi && c.compare(a[i], x) < 0) {
                i++;
            }
            j--;
            while (j >= lo && c.compare(x, a[j]) < 0) {
                j--;
            }
            if (!(i < j)) {
                return i;
            }
            swap(a, i, j);
            i++;
        }
    }


    /**
     * Return the median of the 3 passed in elements.
     * @param lo
     * @param mid
     * @param hi
     * @return
     */
    private static Comparable med3(Comparable lo, Comparable mid, Comparable hi) {
        if (mid.compareTo(lo) < 0) {
            if (hi.compareTo(mid) < 0) {
                return mid;
            } else {
                if (hi.compareTo(lo) < 0) {
                    return hi;
                } else {
                    return lo;
                }
            }
        } else {
            if (hi.compareTo(mid) < 0) {
                if (hi.compareTo(lo) < 0) {
                    return lo;
                } else {
                    return hi;
                }
            } else {
                return mid;
            }
        }
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

    /**
     * Perform a shell sort on array a from indices lo to hi
     * @param a - the array
     * @param lo - Starting element index inclusive
     * @param hi - End element index exclusive
     */
    private static void shellsort(Comparable[] a, int lo, int hi) {
        int size = hi - lo + 1;
        int increm = -1;
        for (int i = INCREMENT.length - 1; i > -1; i--) {
            if (INCREMENT[i] < size) {
                increm = i;
                break;
            }
        }

        while (increm > -1) {
            int increment = INCREMENT[increm];
            int loInc = lo + increment;
            for (int i = loInc; i < hi; i++) {
                int j = i;
                Comparable tmp = a[i];
                while (j >= (loInc) && tmp.compareTo(a[j - increment]) < 0) {
                    a[j] = a[j - increment];
                    j -= increment;
                }
                a[j] = tmp;
            }
            increm--;
        }
    }
    
    private static void shellsort(Object[] a, int lo, int hi, Comparator c) {
        int size = hi - lo + 1;
        int increm = -1;
        for (int i = INCREMENT.length - 1; i > -1; i--) {
            if (INCREMENT[i] < size) {
                increm = i;
                break;
            }
        }

        while (increm > -1) {
            int increment = INCREMENT[increm];
            int loInc = lo + increment;
            for (int i = loInc; i < hi; i++) {
                int j = i;
                Object tmp = a[i];
                while (j >= (loInc) && c.compare(tmp, a[j - increment]) < 0) {
                    a[j] = a[j - increment];
                    j -= increment;
                }
                a[j] = tmp;
            }
            increm--;
        }
    }

    /**
     * Insertion sort.
     * @param a - the array
     * @param lo - Starting element index inclusive
     * @param hi - End element index exclusive
     */
    private static void insertionsort(Comparable[] a, int lo, int hi) {
        int i, j;
        Comparable t;
        for (i = lo; i < hi; i++) {
            j = i;
            t = a[i];
            while (j != lo && t.compareTo(a[j - 1]) < 0) {
                a[j] = a[j - 1];
                j--;
            }
            a[j] = t;
        }
    }
    
    private static void insertionsort(Object[] a, int lo, int hi, Comparator c) {
        int i, j;
        Object t;
        for (i = lo; i < hi; i++) {
            j = i;
            t = a[i];
            while (j != lo && c.compare(t, a[j - 1]) < 0) {
                a[j] = a[j - 1];
                j--;
            }
            a[j] = t;
        }
    }

    /**
     * 2 iterations of bubble sort.
     * One starting at the end of the array and bubbling up.
     * One starting at the beginning of the array and bubbling down.
     * @param a - the array
     * @param lo - Starting element index inclusive
     * @param hi - End element index exclusive
     */
    private static void bubbleBoth(Comparable[] a, int lo, int hi) {
        bubbleDown(a, lo, hi);
        bubbleUp(a, lo, hi);
    }
    
    private static void bubbleBoth(Object[] a, int lo, int hi, Comparator c) {
        bubbleDown(a, lo, hi, c);
        bubbleUp(a, lo, hi, c);
    }

    /**
     * Does one iteration of bubble sort starting at the beginning, and
     * bubbles down until it doesn't do a swap.
     * @param a - the array
     * @param lo - Starting element index inclusive
     * @param hi - End element index exclusive
     */
    private static void bubbleDown(Comparable[] a, int lo, int hi) {
        int i = lo;
        int end = hi - 2;
        while (i < end && a[i].compareTo(a[i + 1]) > 0) {
            swap(a, i, ++i);
        }
    }
    
    private static void bubbleDown(Object[] a, int lo, int hi, Comparator c) {
        int i = lo;
        int end = hi - 2;
        while (i < end && c.compare(a[i], a[i + 1]) > 0) {
            swap(a, i, ++i);
        }
    }

    /**
     * Does one iteration of bubble sort starting at the end and bubbling up
     * to the beginning
     * @param a - the array
     * @param lo - Starting element index inclusive
     * @param hi - End element index exclusive
     */
    private static void bubbleUp(Comparable[] a, int lo, int hi) {
        int i = hi - 1;
        int start = lo;
        while (i > start && a[i].compareTo(a[i - 1]) < 0) {
            swap(a, i, --i);
        }
    }
    
    private static void bubbleUp(Object[] a, int lo, int hi, Comparator c) {
        int i = hi - 1;
        int start = lo;
        while (i > start && c.compare(a[i], a[i - 1]) < 0) {
            swap(a, i, --i);
        }
    }

    private static void swap(Object[] a, int i, int j) {
        Object t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    private static int floorLog2(int a) {
        return (int) (Math.floor(Math.log(a) / Math.log(2)));
    }
}

