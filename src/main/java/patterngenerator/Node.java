package patterngenerator;

import edu.stanford.nlp.ling.IndexedWord;

import java.util.*;

/**
 * @author DANISH AHMED on 12/10/2018
 */
public class Node {
    public IndexedWord indexedNode;
    public String pos;
    public String label;
    public String lemma = null;

    public boolean isMerged = false;
    public List<Integer> mergedIndexes = new ArrayList<>();
    public Set<String> mergedNodes = new HashSet<>();   // sourceLabel/POS typDep>targetLabel/POS

    public Node(IndexedWord indexedNode, String pos, String label) {
        this.indexedNode = indexedNode;
        this.pos = pos;
        this.label = label;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
