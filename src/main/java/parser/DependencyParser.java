package parser;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

import java.util.*;

/**
 * @author DANISH AHMED on 12/7/2018
 */
public class DependencyParser {
    public final List<String> MERGE_TYPED_DEPENDENCIES = new ArrayList<String>(){{
        add("compound");
        add("compound:prt");
        add("name");
        add("mwe");
        add("foreign");
        add("goeswith");

        add("cop");
        add("case");

        /* Noun Dependent word modifiers*/
        add("amod");
        add("neg");
    }};

    public final List<String> SPLIT_TYPED_DEPENDENCIES = new ArrayList<String>(){{
        add("conj");
        add("cc");
        add("cc:preconj");
        add("punct");
    }};

    public Collection<IndexedWord> getRoots(SemanticGraph semanticGraph) {
        return semanticGraph.getRoots();
    }

    public HashMap<IndexedWord, List<SemanticGraphEdge>> getOutEdgesFromRoots (SemanticGraph semanticGraph) {
        HashMap<IndexedWord, List<SemanticGraphEdge>> rootOutEdgesMap = new LinkedHashMap<IndexedWord, List<SemanticGraphEdge>>();
        Collection<IndexedWord> roots = this.getRoots(semanticGraph);
        for (IndexedWord root : roots) {
            List<SemanticGraphEdge> outEdges = semanticGraph.getOutEdgesSorted(root);
            if (outEdges != null && !outEdges.isEmpty())
                rootOutEdgesMap.put(root, outEdges);
        }
        return rootOutEdgesMap;
    }

    public SemanticGraph getGraphBetweenNodes(SemanticGraph originalSG, IndexedWord source, IndexedWord target) {
        List<SemanticGraphEdge> edgePath = originalSG.getShortestUndirectedPathEdges(source, target);
        SemanticGraph semanticGraph = new SemanticGraph();
        IndexedWord root;

        boolean isNull = false;
        if (edgePath ==  null || edgePath.isEmpty())
            isNull = true;
        else {
            for (SemanticGraphEdge edge : edgePath)
                semanticGraph.addEdge(edge);

            for (SemanticGraphEdge edge : edgePath) {
                IndexedWord gov = edge.getGovernor();
                IndexedWord dep = edge.getDependent();

                List<SemanticGraphEdge> govOutEdges = originalSG.getOutEdgesSorted(gov);
                List<SemanticGraphEdge> depOutEdges = originalSG.getOutEdgesSorted(dep);
                for (SemanticGraphEdge govEdge : govOutEdges) {
                    IndexedWord outNode = govEdge.getDependent();
                    if (outNode != dep
                            && this.MERGE_TYPED_DEPENDENCIES.contains(govEdge.getRelation().getShortName())
                            && !semanticGraph.containsEdge(govEdge.getGovernor(), outNode)) {
                        semanticGraph.addEdge(originalSG.getEdge(govEdge.getGovernor(), outNode));
                    }
                }

                for (SemanticGraphEdge depEdges : depOutEdges) {
                    IndexedWord outNode = depEdges.getDependent();
                    if (this.MERGE_TYPED_DEPENDENCIES.contains(depEdges.getRelation().getShortName())
                            && !semanticGraph.containsEdge(depEdges.getGovernor(), outNode)) {
                        semanticGraph.addEdge(originalSG.getEdge(depEdges.getGovernor(), outNode));
                    }
                }
            }
            root = getRootWord(semanticGraph);
            if (root == null)
                isNull = true;
            else
                semanticGraph.addRoot(root);
        }

        if (isNull) {
            semanticGraph = generateSemanticGraphFromRoot(originalSG, source, target);
            if (semanticGraph == null)
                return null;
        }

        return semanticGraph;
    }

    public SemanticGraph generateSemanticGraphFromRoot(SemanticGraph semanticGraph, IndexedWord source, IndexedWord target) {
        Collection<IndexedWord> roots = semanticGraph.getRoots();
        SemanticGraph sg = new SemanticGraph();
        for (IndexedWord root : roots) {
            List<SemanticGraphEdge> sourceEdgePath = semanticGraph.getShortestDirectedPathEdges(root, source);
            List<SemanticGraphEdge> targetEdgePath = semanticGraph.getShortestDirectedPathEdges(root, target);

            if (sourceEdgePath.size() > 0 && targetEdgePath.size() > 0) {
                sg.addRoot(root);

                for (SemanticGraphEdge sourceEdge : sourceEdgePath)
                    sg.addEdge(sourceEdge);
                for (SemanticGraphEdge targetEdge : targetEdgePath)
                    sg.addEdge(targetEdge);
            }
        }

        if (sg.getFirstRoot() == null)
            return null;
        return sg;
    }

    public IndexedWord getRootWord(SemanticGraph semanticGraph) {
        // has no incoming edge but has outgoing edges
        IndexedWord root = null;
        List<SemanticGraphEdge> allEdges = semanticGraph.edgeListSorted();
        for (SemanticGraphEdge edge : allEdges) {
            IndexedWord gov = edge.getGovernor();
            IndexedWord dep = edge.getDependent();

            if ((semanticGraph.getIncomingEdgesSorted(gov) == null
                    || semanticGraph.getIncomingEdgesSorted(gov).isEmpty())
                    && (semanticGraph.getOutEdgesSorted(gov) != null
                    && !semanticGraph.getOutEdgesSorted(gov).isEmpty()))
                root = gov;
            else if ((semanticGraph.getIncomingEdgesSorted(dep) == null
                    || semanticGraph.getIncomingEdgesSorted(dep).isEmpty())
                    && (semanticGraph.getOutEdgesSorted(dep) != null
                    && !semanticGraph.getOutEdgesSorted(dep).isEmpty()))
                root = dep;
        }
        return root;
    }

    public Set<SemanticGraph> removeDuplicatedGraphs(Set<SemanticGraph> sgsGeneratedFromSameSubjObj) {
        Set<SemanticGraph> filteredGraphs = new HashSet<>();
        Set<IndexedWord> distinctRoots = new HashSet<>();
        for (SemanticGraph sg : sgsGeneratedFromSameSubjObj)
            distinctRoots.add(sg.getFirstRoot());

        if (distinctRoots.size() == 1)
            return sgsGeneratedFromSameSubjObj;

        for (SemanticGraph sg : sgsGeneratedFromSameSubjObj) {
            boolean removeSG = false;
            IndexedWord sgRoot = sg.getFirstRoot();

            for (IndexedWord root : distinctRoots) {
                if (root == sgRoot)
                    continue;
                if (sg.containsVertex(root)) {
                    removeSG = true;
                    break;
                }
            }
            if (!removeSG)
                filteredGraphs.add(sg);
        }
        return filteredGraphs;
    }

    public Set<IndexedWord> getRootLessNodes(SemanticGraph semanticGraph, IndexedWord root) {
        Set<IndexedWord> allIWFromRoot = semanticGraph.vertexSet();
        Set<IndexedWord> rootLessNodes = new HashSet<>();

        for (IndexedWord vertex : allIWFromRoot) {
            if (vertex == root)
                continue;
            List<SemanticGraphEdge> edgesToRoot = semanticGraph.getShortestDirectedPathEdges(root, vertex);
            if (edgesToRoot == null || edgesToRoot.size() == 0)
                rootLessNodes.add(vertex);
        }
        return rootLessNodes;
    }
}
