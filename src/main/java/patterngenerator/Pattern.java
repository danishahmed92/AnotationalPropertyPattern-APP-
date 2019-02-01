package patterngenerator;

import config.IniConfig;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import nlp.POSLemma;
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
    public Set<String> distinctVerbs = new HashSet<>();

    public Pattern(SemanticGraph semanticGraph) {
        try {
            this.semanticGraph = semanticGraph;
            this.sgPretty = semanticGraph.toCompactString(true);
            this.sgToSentence = semanticGraph.toRecoveredSentenceString();

            IndexedWord root = semanticGraph.getFirstRoot();
            String rootPOS = root.tag();
            String rootLabel = root.backingLabel().word();
//            String rootLabel = root.backingLabel().originalText();
            String rootLemma;
            if (rootPOS.contains("NN")) {
                rootLemma = WordNet.wordNet.getVerbForNoun(root.backingLabel().originalText());

                Annotation newLemmaAnno = POSLemma.PLInstance.annotateDocument(rootLemma);
                List<CoreMap> sentences = newLemmaAnno.get(CoreAnnotations.SentencesAnnotation.class);
                rootLemma = sentences.get(0).get(CoreAnnotations.TokensAnnotation.class).get(0).lemma();
            } else {
                rootLemma = root.lemma();
            }

            this.root = new Node(root, rootPOS, rootLabel);
            this.root.setLemma(rootLemma);

            setSubjObjPath();

            subjPatternStr = String.valueOf(setPatternStr(rootToSubjPath));
            objPatternStr = String.valueOf(setPatternStr(rootToObjPath));
            setDistinctNouns();
            setDistinctVerbs();
            setMergePatternStr();
        } catch (NullPointerException npe) {
            npe.printStackTrace();

            this.root = null;
            this.semanticGraph = null;
            this.rootToSubjPath = null;
            this.rootToObjPath = null;
            this.subjPatternStr = null;
            this.objPatternStr = null;
            this.mergePatternStr = null;
            this.sgPretty = null;
            this.sgToSentence = null;
            this.distinctNouns = null;
        }
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

    public void setDistinctNouns() {
        Set<IndexedWord> vertexSet = semanticGraph.vertexSet();
        for (IndexedWord iw : vertexSet) {
            if (iw.tag().contains("NN")
                && !iw.backingLabel().originalText().equals("%R%")
                && !iw.backingLabel().originalText().equals("%D%")
                && !iw.ner().toLowerCase().contains("person")
                    // handling 's case
                && !iw.backingLabel().word().toLowerCase().equals("s") ) {
                distinctNouns.add(iw.backingLabel().word());
            }
        }
    }

    public void setDistinctVerbs() {
        Set<IndexedWord> vertexSet = semanticGraph.vertexSet();
        for (IndexedWord iw : vertexSet) {
            if ((iw.tag().contains("VB") || iw.tag().contains("JJ"))
                    && !iw.backingLabel().originalText().equals("%R%")
                    && !iw.backingLabel().originalText().equals("%D%")

//                    Should not be a stop word
                    && !IniConfig.configInstance.stopWordsSet.contains(iw.backingLabel().word().toLowerCase())) {
                distinctVerbs.add(iw.backingLabel().word());
            }
        }
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

            /*if (!gov.equals(root.indexedNode))
                addNoun(gov);
            if (!dep.equals(root.indexedNode))
                addNoun(dep);*/

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
        List<SemanticGraphEdge> allEdges = semanticGraph.edgeListSorted();
        if (allEdges ==  null || allEdges.isEmpty())
            return;

        Set<IndexedWord> subjIWList = new HashSet<>();
        Set<IndexedWord> objIWList = new HashSet<>();
        for (SemanticGraphEdge edge : allEdges) {
            IndexedWord gov = edge.getGovernor();
            IndexedWord dep = edge.getDependent();

            if (gov.originalText().equals("%D%"))
                subjIWList.add(gov);
            else if (gov.originalText().equals("%R%"))
                objIWList.add(gov);

            if (dep.originalText().equals("%D%"))
                subjIWList.add(dep);
            else if (dep.originalText().equals("%R%"))
                objIWList.add(dep);
        }

        IndexedWord maxSubj = null;
        int maxSubjDepth = -1;
        for (IndexedWord node : subjIWList) {
            int currentDepth = semanticGraph.getShortestUndirectedPathEdges(root.indexedNode, node).size();
            if (currentDepth > maxSubjDepth) {
                maxSubj = node;
                maxSubjDepth = currentDepth;
            }
        }
        rootToSubjPath = semanticGraph.getShortestUndirectedPathEdges(root.indexedNode, maxSubj);

        IndexedWord maxObj = null;
        int maxObjDepth = -1;
        for (IndexedWord node : objIWList) {
            int currentDepth = semanticGraph.getShortestUndirectedPathEdges(root.indexedNode, node).size();
            if (currentDepth > maxObjDepth) {
                maxObj = node;
                maxObjDepth = currentDepth;
            }
        }
        rootToObjPath = semanticGraph.getShortestUndirectedPathEdges(root.indexedNode, maxObj);
    }
}
