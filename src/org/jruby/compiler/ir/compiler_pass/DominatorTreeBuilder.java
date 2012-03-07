package org.jruby.compiler.ir.compiler_pass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.Tuple;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class DominatorTreeBuilder extends CompilerPass {
    private static String[] NAMES = new String[] {"build_dominator", "dominator"};
    private static final Logger LOG = LoggerFactory.getLogger("DominatorTreeBuilder");
    public static List<Tuple<Class<CompilerPass>, DependencyType>> DEPENDENCIES = new ArrayList<Tuple<Class<CompilerPass>, DependencyType>>() {{
       add(new Tuple(CFGBuilder.class, CompilerPass.DependencyType.RETRIEVE)); 
    }};
    
    public String getLabel() {
        return "Build Dominator Tree";
    }
    
    public boolean isPreOrder() {
        return false;
    }
    
    @Override
    public List<Tuple<Class<CompilerPass>, DependencyType>> getDependencies() {
        return DEPENDENCIES;
    }    

    public Object execute(IRScope scope, Object... data) {
        CFG cfg = (CFG) data[0];

        try {
            buildDominatorTree(cfg, cfg.postOrderList(), cfg.getMaxNodeID());
        } catch (Exception e) {
            LOG.debug("Caught exception building dom tree for {}", scope.cfg());
        }
        
        return null;
    }
    
    public void buildDominatorTree(CFG cfg, LinkedList<BasicBlock> postOrderList, int maxNodeId) {
        Integer[] bbToPoNumbers = new Integer[maxNodeId + 1]; // Set up a map of bbid -> post order numbering
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
        Integer[] idoms = new Integer[maxNodeId + 1];

        BasicBlock root = cfg.getEntryBB();
        Integer rootPoNumber = bbToPoNumbers[root.getID()];
        idoms[rootPoNumber] = rootPoNumber;

        boolean changed = true;
        while (changed) {
            changed = false;
            it = postOrderList.listIterator(cfg.size());
            while (it.hasPrevious()) {
                BasicBlock b = it.previous();
                if (b == root) continue;

                // Non-root -- process it
                Integer bPoNumber = bbToPoNumbers[b.getID()];
                Integer oldBIdom = idoms[bPoNumber];
                Integer newBIdom = null;

                // newBIdom is initialized to be some (first-encountered, for ex.) processed predecessor of 'b'.
                for (BasicBlock src : cfg.getIncomingSources(b)) {
                    Integer srcPoNumber = bbToPoNumbers[src.getID()];
                    
                    if (idoms[srcPoNumber] != null) {
//                        System.out.println("Initialized idom(" + bPoNumber + ")=" + srcPoNumber);
                        newBIdom = srcPoNumber;
                        break;
                    }
                }

                // newBIdom should not be null
                assert newBIdom != null;

                // Now, intersect dom sets of all of b's predecessors 
                Integer processedPred = newBIdom;
                for (BasicBlock src: cfg.getIncomingSources(b)) {
                    // Process b's predecessors except the initialized bidom value
                    Integer srcPoNumber = bbToPoNumbers[src.getID()];
                    Integer srcIdom = idoms[srcPoNumber];
                    if ((srcIdom != null) && (srcPoNumber != processedPred)) {
//                        Integer old = newBIdom;
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
        for (Integer i = 0; i < maxNodeId; i++) {
            idomMap.put(poNumbersToBB[i], poNumbersToBB[idoms[i]]);
//            System.out.println("IDOM(" + poNumbersToBB[i].getID() + ") = " + poNumbersToBB[idoms[i]].getID());
        }
    }
    
    private Integer intersectDomSets(Integer[] idomMap, Integer nb1, Integer nb2) {
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
