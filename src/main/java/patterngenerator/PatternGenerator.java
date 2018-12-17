package patterngenerator;

import annotation.DependencyTree;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import parser.DependencyParser;

import java.util.*;

/**
 * @author DANISH AHMED on 12/7/2018
 */
public class PatternGenerator {
    public static SemanticGraph replaceDomainRange(SemanticGraph semanticGraph, String subj, String obj) {
        SemanticGraph sg = new SemanticGraph();
        List<SemanticGraphEdge> allEdges = semanticGraph.edgeListSorted();
        if (allEdges ==  null || allEdges.isEmpty())
            return null;

        List<IndexedWord> subjIWList = DependencyTree.getIndexedWordsFromString(semanticGraph, subj);
        List<IndexedWord> objIWList = DependencyTree.getIndexedWordsFromString(semanticGraph, obj);
        for (SemanticGraphEdge edge : allEdges) {
            IndexedWord gov = edge.getGovernor();
            IndexedWord dep = edge.getDependent();

            if (!subjIWList.contains(gov) && !objIWList.contains(gov)
                    && !subjIWList.contains(dep) && !objIWList.contains(dep)) {
                sg.addEdge(edge);
            } else {
                if (subjIWList.contains(gov))
                    edge.getGovernor().setOriginalText("%D%");
                if (objIWList.contains(gov))
                    edge.getGovernor().setOriginalText("%R%");
                if (subjIWList.contains(dep))
                    edge.getDependent().setOriginalText("%D%");
                if (objIWList.contains(dep))
                    edge.getDependent().setOriginalText("%R%");

                sg.addEdge(edge);
            }
        }

        DependencyParser dp = new DependencyParser();
        IndexedWord root = dp.getRootWord(sg);
        sg.setRoot(root);

        return sg;
    }

    public static SemanticGraph pruneGraph(SemanticGraph semanticGraph, String subj, String obj) {
        List<IndexedWord> subjIWList = DependencyTree.getIndexedWordsFromString(semanticGraph, subj);
        List<IndexedWord> objIWList = DependencyTree.getIndexedWordsFromString(semanticGraph, obj);
        IndexedWord root = semanticGraph.getFirstRoot();

        DependencyParser dp = new DependencyParser();
        HashMap<String, Set<SemanticGraphEdge>> subjRemoveAddMap = setEdgesRemovalAddition(semanticGraph, subjIWList);
        HashMap<String, Set<SemanticGraphEdge>> objRemoveAddMap = setEdgesRemovalAddition(semanticGraph, objIWList);

        for (SemanticGraphEdge edge : subjRemoveAddMap.get("add"))
            semanticGraph.addEdge(edge);

        for (SemanticGraphEdge edge : objRemoveAddMap.get("add"))
            semanticGraph.addEdge(edge);

        for (SemanticGraphEdge edge : subjRemoveAddMap.get("remove"))
            semanticGraph.removeEdge(edge);

        for (SemanticGraphEdge edge : objRemoveAddMap.get("remove"))
            semanticGraph.removeEdge(edge);

        Set<IndexedWord> rootLessNodes = dp.getRootLessNodes(semanticGraph, root);
        for (IndexedWord node : rootLessNodes)
            semanticGraph.removeVertex(node);

        return semanticGraph;
    }

//    TODO: ISSUE: When there are 2 or consecutive nmod (other than :of)
//    Then prevEdge will be set to last removed edge
//    and when you will remove the vertices that are not connected to root, then this node will also be removed.
//    UPDATE: I think it won't cause issue as prevEdge is set to edge after modification,
//    so governor will always have a path.
    private static HashMap<String, Set<SemanticGraphEdge>> setEdgesRemovalAddition(SemanticGraph semanticGraph, List<IndexedWord> iWList) {
        HashMap<String, Set<SemanticGraphEdge>> edgeRemoveAddMap = new LinkedHashMap<>();

        Set<SemanticGraphEdge> toRemoveEdgeList = new HashSet<>();
        Set<SemanticGraphEdge> toAddEdgeList = new HashSet<>();

        IndexedWord root = semanticGraph.getFirstRoot();
        for (IndexedWord iW : iWList) {
            List<SemanticGraphEdge> rootToNodePath = semanticGraph.getShortestDirectedPathEdges(root, iW);
            String prevRel = "";
            SemanticGraphEdge prevEdge = null;
            boolean isNmod = false;
            for (SemanticGraphEdge edge : rootToNodePath) {
                String rel = edge.getRelation().getShortName();
                String specific = edge.getRelation().getSpecific();
                if (specific != null && specific.length() > 0)
                    rel = rel + ":" + specific;

                if (rel.contains("nmod:of") && isNmod) {
                    IndexedWord gov = edge.getGovernor();
                    IndexedWord dep = edge.getDependent();
                    if (!iWList.contains(gov) && !iWList.contains(dep))
                        toRemoveEdgeList.add(prevEdge);
                    continue;
                }
                if (rel.contains("nmod:") && rel.equals(prevRel)) {
                    toRemoveEdgeList.add(prevEdge);

                    SemanticGraphEdge toAddEdge = new SemanticGraphEdge(
                            prevEdge.getGovernor(), edge.getDependent(),
                            prevEdge.getRelation(),
                            edge.getWeight(),
                            edge.isExtra()
                    );
                    toAddEdgeList.add(toAddEdge);
                }

                if (!rel.equals("nmod:of")) {
                    prevEdge = edge;
                    prevRel = rel;
                    isNmod = rel.contains("nmod:");
                }
            }
        }
        edgeRemoveAddMap.put("add", toAddEdgeList);
        edgeRemoveAddMap.put("remove", toRemoveEdgeList);

        return edgeRemoveAddMap;
    }

    public static Set<SemanticGraph> removeSubContainPatterns(Set<SemanticGraph> sgPatterns) {
        if (sgPatterns.size() <= 1)
            return sgPatterns;

        HashMap<IndexedWord, Set<SemanticGraph>> rootGraphsMap = new LinkedHashMap<>();
        for (SemanticGraph sg : sgPatterns) {
            IndexedWord root = sg.getFirstRoot();
            Set<SemanticGraph> graphs;
            if (!rootGraphsMap.containsKey(root)) {
                graphs = new HashSet<>();
                graphs.add(sg);
            } else {
                graphs = rootGraphsMap.get(root);
                graphs.add(sg);
            }
            rootGraphsMap.put(root, graphs);
        }

        Set<SemanticGraph> nonSubContainGraphs = new HashSet<>();
        for (IndexedWord root : rootGraphsMap.keySet()) {
            int maxEdges = -1;
            SemanticGraph maxGraph = null;
            for (SemanticGraph sg : rootGraphsMap.get(root)) {
                int edges = sg.edgeCount();
                if (edges >= maxEdges) {
                    maxEdges = edges;
                    maxGraph = sg;
                }
            }
            nonSubContainGraphs.add(maxGraph);
        }
        return nonSubContainGraphs;
    }
}
