package patterngenerator;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import parser.DependencyParser;
import wordnet.WordNet;

import java.util.*;

/**
 * @author DANISH AHMED on 12/10/2018
 */
public class Pattern {
    public Node root;
    public SemanticGraph semanticGraph;

    public List<SemanticGraphEdge> rootToSubjPath;
    public List<SemanticGraphEdge> rootToObjPath;

    public String subjPatternStr;
    public String objPatternStr;
    public String mergePatternStr;

    public String sgPretty;
    public String sgToSentence;

    public Set<String> distinctNouns = new HashSet<>();

    public Pattern(SemanticGraph semanticGraph) {
        this.semanticGraph = semanticGraph;
        this.sgPretty = semanticGraph.toCompactString(true);
        this.sgToSentence = semanticGraph.toRecoveredSentenceString();

        IndexedWord root = semanticGraph.getFirstRoot();
        String rootPOS = root.tag();
        String rootLabel = root.backingLabel().originalText();
        String rootLemma;
        if (rootPOS.contains("NN")) {
            rootLemma = WordNet.wordNet.getVerbForNoun(root.backingLabel().originalText());
        } else {
            rootLemma = root.lemma();
        }

        this.root = new Node(root, rootPOS, rootLabel);
        this.root.setLemma(rootLemma);

        setSubjObjPath();
        subjPatternStr = String.valueOf(setPatternStr(rootToSubjPath));
        objPatternStr = String.valueOf(setPatternStr(rootToObjPath));
        setMergePatternStr();

        System.out.println(mergePatternStr);
    }

    public void setMergePatternStr() {
        final String patternFormat = "{%s}%s{%s}";
        mergePatternStr = String.format(patternFormat,
                subjPatternStr,
                root.lemma,
                objPatternStr);
    }

    public void addNoun(IndexedWord iw) {
        if (iw.tag().equals("NN"))
            distinctNouns.add(WordNet.wordNet.getVerbForNoun(iw.toString()));
    }

    public StringBuilder setPatternStr(List<SemanticGraphEdge> path) {
        final String patternFormat = "(%s)-%s>%s";
        StringBuilder pattern = new StringBuilder();
        String lastTag = "";

        DependencyParser dp = new DependencyParser();
        List<String> mergeRelList = dp.MERGE_TYPED_DEPENDENCIES;
        for (SemanticGraphEdge edge : path) {
            IndexedWord gov = edge.getGovernor();
            IndexedWord dep = edge.getDependent();

            String depStr = (dep.originalText().equals("%D%") || dep.originalText().equals("%R%")) ?
                    dep.originalText() : "";
            String rel = edge.getRelation().getShortName();
            String specific = edge.getRelation().getSpecific();
            if (specific != null && specific.length() > 0)
                rel = rel + ":" + specific;

            if (!gov.equals(root.indexedNode))
                addNoun(gov);
            if (!dep.equals(root.indexedNode))
                addNoun(dep);

            if (mergeRelList.contains(rel) &&
                    ((gov.tag().equals(dep.tag()))
                    || gov.tag().equals("JJ")
                    || dep.tag().equals("JJ")))
                continue;

            pattern.append(String.format(patternFormat,
                    gov.tag(),
                    rel,
                    depStr));
            lastTag = "(" + dep.tag() + ")";
        }
        pattern.append(lastTag);
        return pattern;
    }

    public void setSubjObjPath() {
        // the root node will always have 2 out-edges at max
        List<SemanticGraphEdge> rootOutEdges = semanticGraph.getOutEdgesSorted(root.indexedNode);
        IndexedWord lastSubjIW = null;
        IndexedWord lastObjIW = null;
        for (SemanticGraphEdge outEdge : rootOutEdges) {
            IndexedWord dep;
            dep = outEdge.getDependent();

            List<SemanticGraphEdge> currentOutEdges;
            do {
                if (dep.originalText().equals("%D%"))
                    lastSubjIW = dep;
                else if (dep.originalText().equals("%R%"))
                    lastObjIW = dep;

                currentOutEdges = semanticGraph.getOutEdgesSorted(dep);
                if (currentOutEdges != null && currentOutEdges.size() > 0)
                    dep = currentOutEdges.get(0).getDependent();
            } while (currentOutEdges != null && currentOutEdges.size() > 0);
        }

        rootToSubjPath = semanticGraph.getShortestUndirectedPathEdges(root.indexedNode, lastSubjIW);
        rootToObjPath = semanticGraph.getShortestUndirectedPathEdges(root.indexedNode, lastObjIW);
    }
}
