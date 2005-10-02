package org.jruby.util;

import java.util.ArrayList;
import java.util.List;

import org.jruby.ast.Node;

public class ListUtil {

    public static List create(Node node1, Node node2) {
        ArrayList list = new ArrayList();
        list.add(node1);
        list.add(node2);
        return list;
    }

    public static List create(Node node1, Node node2, Node node3) {
        List list = create(node1, node2);
        list.add(node3);
        return list;
    }

}
