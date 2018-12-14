package patterngenerator;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import wordnet.WordNet;

import java.util.Set;

/**
 * @author DANISH AHMED on 12/10/2018
 */
public class Pattern {
    public Node root;
    public SemanticGraph semanticGraph;

    public String subjLabel;
    public String objLabel;

    public Pattern(SemanticGraph semanticGraph) {
        this.semanticGraph = semanticGraph;

        IndexedWord root = semanticGraph.getFirstRoot();
        String rootPOS = root.tag();
        String rootLabel = root.backingLabel().originalText();
        String rootLemma;
        if (rootPOS.contains("NN")) {
            rootLemma = WordNet.wordNet.getVerbForNoun(root.backingLabel().originalText());
        } else {
            rootLemma = root.lemma();
        }

        this.root = new Node(root, rootLabel);
        this.root.setLemma(rootLemma);
    }

    public void generatePattern() {
        Set<IndexedWord> leafVertices = semanticGraph.getLeafVertices();
//        leafVertices count will always be 2 because you extracted path (linear) by selecting source & target IWs.


    }
}
