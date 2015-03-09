package org.jruby.ir.passes;

import org.jruby.ir.IRScope;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.util.*;

/* SSS: Currently unused code. Will be useful for some SSA building algos. */
public class DominatorTreeBuilder extends CompilerPass {
    private static int NULL = -1;
    private static final Logger LOG = LoggerFactory.getLogger("DominatorTreeBuilder");

    @Override
    public String getLabel() {
        return "Build Dominator Tree";
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        CFG cfg = (CFG) data[0];

        try {
            buildDominatorTree(cfg, cfg.postOrderList(), cfg.getMaxNodeID());
        } catch (Exception e) {
            LOG.debug("Caught exception building dom tree for {}", scope.getCFG());
        }

        return null;
    }

    @Override
    public boolean invalidate(IRScope scope) {
        return false;
    }

    public void buildDominatorTree(CFG cfg, LinkedList<BasicBlock> postOrderList, int maxNodeId) {
        int[] bbToPoNumbers = new int[maxNodeId + 1]; // Set up a map of bbid -> post order numbering
        BasicBlock[] poNumbersToBB = new BasicBlock[maxNodeId + 1];
        int n = 0;
        ListIterator<BasicBlock> it = postOrderList.listIterator();
        while (it.hasNext()) {
            BasicBlock b = it.next();
            bbToPoNumbers[b.getID()] = n;
            poNumbersToBB[n] = b;
            n++;
        }

        // Construct the dominator sets using the fast dominance algorithm by
        // Keith D. Cooper, Timothy J. Harvey, and Ken Kennedy.
        // http://www.cs.rice.edu/~keith/EMBED/dom.pdf (tip courtesy Slava Pestov)
        //
        // Faster than the standard iterative data-flow algorithm
        //
        // This maps a bb's post-order number to the bb's idom post-order number.
        // We convert this po-number -> po-number map to a bb -> bb map later on!
        int[] idoms = new int[maxNodeId + 1];

        BasicBlock root = cfg.getEntryBB();
        int rootPoNumber = bbToPoNumbers[root.getID()];
        idoms[rootPoNumber] = rootPoNumber;

        boolean changed = true;
        while (changed) {
            changed = false;
            it = postOrderList.listIterator(cfg.size());
            while (it.hasPrevious()) {
                BasicBlock b = it.previous();
                if (b == root) continue;

                // Non-root -- process it
                int bPoNumber = bbToPoNumbers[b.getID()];
                int oldBIdom = idoms[bPoNumber];
                int newBIdom = NULL;

                // newBIdom is initialized to be some (first-encountered, for ex.) processed predecessor of 'b'.
                for (BasicBlock src : cfg.getIncomingSources(b)) {
                    int srcPoNumber = bbToPoNumbers[src.getID()];

                    if (idoms[srcPoNumber] != NULL) {
//                        System.out.println("Initialized idom(" + bPoNumber + ")=" + srcPoNumber);
                        newBIdom = srcPoNumber;
                        break;
                    }
                }

                // newBIdom should not be null
                assert newBIdom != NULL;

                // Now, intersect dom sets of all of b's predecessors
                int processedPred = newBIdom;
                for (BasicBlock src: cfg.getIncomingSources(b)) {
                    // Process b's predecessors except the initialized bidom value
                    int srcPoNumber = bbToPoNumbers[src.getID()];
                    int srcIdom = idoms[srcPoNumber];
                    if ((srcIdom != NULL) && (srcPoNumber != processedPred)) {
//                        int old = newBIdom;
                        newBIdom = intersectDomSets(idoms, srcPoNumber, newBIdom);
//                        System.out.println("Intersect " + srcIdom + " & " + old + " = " + newBIdom);
                    }
                }

                // Has something changed?
                if (oldBIdom != newBIdom) {
                    changed = true;
                    idoms[bPoNumber] = newBIdom;
//                    System.out.println("Changed: idom(" + bPoNumber + ")= " + newBIdom);
                }
            }
        }

        // Convert the idom map based on post order numbers to one based on basic blocks
        Map<BasicBlock, BasicBlock> idomMap = new HashMap<BasicBlock, BasicBlock>();
        for (int i = 0; i < maxNodeId; i++) {
            idomMap.put(poNumbersToBB[i], poNumbersToBB[idoms[i]]);
//            System.out.println("IDOM(" + poNumbersToBB[i].getID() + ") = " + poNumbersToBB[idoms[i]].getID());
        }
    }

    private int intersectDomSets(int[] idomMap, int nb1, int nb2) {
        while (nb1 != nb2) {
            while (nb1 < nb2) {
                nb1 = idomMap[nb1];
            }
            while (nb2 < nb1) {
                nb2 = idomMap[nb2];
            }
        }

        return nb1;
    }
}
