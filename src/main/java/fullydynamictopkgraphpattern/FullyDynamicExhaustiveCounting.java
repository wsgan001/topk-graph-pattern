package fullydynamictopkgraphpattern;

import java.util.Set;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import input.StreamEdge;
import struct.GraphPattern;
import struct.LabeledNeighbor;
import struct.LabeledNode;
import struct.NodeMap;
import struct.Triplet;
import topkgraphpattern.TopkGraphPatterns;
import utility.EdgeHandler;
import utility.SetFunctions;

public class FullyDynamicExhaustiveCounting implements TopkGraphPatterns{
	NodeMap nodeMap;
	EdgeHandler utility;
	THashMap<Triplet, Integer> counter;
	THashMap<GraphPattern, Integer> frequentPatterns;
	int numSubgraph;
	public FullyDynamicExhaustiveCounting() {
		this.nodeMap = new NodeMap();
		utility = new EdgeHandler();
		counter = new THashMap<Triplet, Integer>();
		numSubgraph = 0 ;
		frequentPatterns = new THashMap<GraphPattern, Integer>();
	}
	public boolean addEdge(StreamEdge edge) {
		//System.out.println("+" + edge);
		if(nodeMap.contains(edge))
			return false;
		
		//System.out.println(nodeMap.map);
		LabeledNode src = new LabeledNode(edge.getSource(), edge.getSrcLabel());
		LabeledNode dst = new LabeledNode(edge.getDestination(),edge.getDstLabel());

		THashSet<LabeledNeighbor> srcNeighbor = nodeMap.getNeighbors(src);
		THashSet<LabeledNeighbor> dstNeighbor = nodeMap.getNeighbors(dst);

		SetFunctions<LabeledNeighbor> functions = new SetFunctions<LabeledNeighbor>();
		Set<LabeledNeighbor> common = functions.intersectionSet(srcNeighbor, dstNeighbor);
		
		THashMap<LabeledNeighbor, LabeledNeighbor> srcCommonNeighbor = new THashMap<LabeledNeighbor, LabeledNeighbor>();

		//iterate through source neighbors;
		for(LabeledNeighbor t: srcNeighbor) {
			if(!common.contains(t)) {
				Triplet triplet = new Triplet(src, dst, t.getDst(),edge, new StreamEdge(src.getVertexId(), src.getVertexLabel(), t.getDst().getVertexId(), t.getDst().getVertexLabel(), t.getEdgeLabel()));
				addSubgraph(triplet);
			} else {
				srcCommonNeighbor.put(t, t);
			}
		}

		//iteration through destination neighbors
		for(LabeledNeighbor t: dstNeighbor) {
			if(!common.contains(t)) {
				Triplet triplet = new Triplet(src, dst, t.getDst(),edge, new StreamEdge(dst.getVertexId(), dst.getVertexLabel(), t.getDst().getVertexId(),t.getDst().getVertexLabel(), t.getEdgeLabel()));
				addSubgraph(triplet);
			}else {
				LabeledNeighbor srcComNeighbor = srcCommonNeighbor.get(t);
				LabeledNode a = src;
				LabeledNode b = dst;
				LabeledNode c = t.getDst();
				StreamEdge edgeA = edge;
				StreamEdge edgeB = new StreamEdge(t.getDst().getVertexId(),t.getDst().getVertexLabel(),  src.getVertexId(),src.getVertexLabel(), srcComNeighbor.getEdgeLabel());
				StreamEdge edgeC = new StreamEdge(t.getDst().getVertexId(), t.getDst().getVertexLabel(), dst.getVertexId(), dst.getVertexLabel(), t.getEdgeLabel());

				Triplet tripletWedge = new Triplet(a, b, c, edgeB, edgeC );
				removeSubgraph(tripletWedge);

				Triplet tripletTriangle = new Triplet(a, b, c,edgeA, edgeB, edgeC );
				addSubgraph(tripletTriangle);

			}
		}

		utility.handleEdgeAddition(edge, nodeMap);
		//System.out.println(counter);
		return false;
	}

	public boolean removeEdge(StreamEdge edge) {
		if(!nodeMap.contains(edge))
			return false;
		//System.out.println("-" + edge);
		utility.handleEdgeDeletion(edge, nodeMap);
		
		LabeledNode src = new LabeledNode(edge.getSource(), edge.getSrcLabel());
		LabeledNode dst = new LabeledNode(edge.getDestination(),edge.getDstLabel());

		THashSet<LabeledNeighbor> srcNeighbor = nodeMap.getNeighbors(src);
		THashSet<LabeledNeighbor> dstNeighbor = nodeMap.getNeighbors(dst);

		SetFunctions<LabeledNeighbor> functions = new SetFunctions<LabeledNeighbor>();
		Set<LabeledNeighbor> common = functions.intersectionSet(srcNeighbor, dstNeighbor);
		
		THashMap<LabeledNeighbor, LabeledNeighbor> commonNeighbor = new THashMap<LabeledNeighbor, LabeledNeighbor>();
		
		//iterate through source neighbors
		for(LabeledNeighbor t: srcNeighbor) {
			if(!common.contains(t)) {
				Triplet triplet = new Triplet(src, dst, t.getDst(),edge, new StreamEdge(src.getVertexId(),src.getVertexLabel(), t.getDst().getVertexId(),t.getDst().getVertexLabel(), t.getEdgeLabel()));
				removeSubgraph(triplet);
			} else {
				commonNeighbor.put(t, t);
			}
		}

		//iterate through destination neighbors
		for(LabeledNeighbor t: dstNeighbor) {
			if(!common.contains(t)) {
				Triplet triplet = new Triplet(src, dst, t.getDst(),edge, new StreamEdge(dst.getVertexId(), dst.getVertexLabel(), t.getDst().getVertexId(), t.getDst().getVertexLabel(), t.getEdgeLabel()));
				removeSubgraph(triplet);
			}else {
				LabeledNeighbor comNeighbor = commonNeighbor.get(t);
				LabeledNode a = src;
				LabeledNode b = dst;
				LabeledNode c = t.getDst();
				
				StreamEdge edgeA = edge;
				StreamEdge edgeB = new StreamEdge(c.getVertexId() , c.getVertexLabel(), src.getVertexId(), src.getVertexLabel(), comNeighbor.getEdgeLabel());
				StreamEdge edgeC = new StreamEdge(c.getVertexId(), c.getVertexLabel(), dst.getVertexId(),dst.getVertexLabel(), t.getEdgeLabel());
				
				Triplet tripletTriangle = new Triplet(a, b, c,edgeA, edgeB, edgeC );
				removeSubgraph(tripletTriangle);
				
				Triplet tripletWedge = new Triplet(a, b, c, edgeB, edgeC );
				addSubgraph(tripletWedge);
			}
		}
		//System.out.println(counter);
		return true;
	}
	void removeSubgraph(Triplet t) {
		/*if(counter.containsKey(t)) {
			int count = counter.get(t);
			if(count > 1)
				counter.put(t, count-1);
			else 
				counter.remove(t);
			numSubgraph--;
			removeFrequentPattern(t);
		}
		else {
			System.out.println("remove error " + t);
			System.out.println(counter);
			System.exit(1);
		}*/
		//removeFrequentPattern(t);
		numSubgraph--;
		removeFrequentPattern(t);
		
	}
	
	void addSubgraph(Triplet t) {
	/*	if(counter.containsKey(t)) {
			int count = counter.get(t);
			counter.put(t, count+1);
		}else {
			counter.put(t, 1);
		}
		*/
		addFrequentPattern(t);
		numSubgraph++;
	}
	
	void addFrequentPattern(Triplet t) {
		GraphPattern p = new GraphPattern(t);
		if(frequentPatterns.contains(p)) {
			int count = frequentPatterns.get(p);
			frequentPatterns.put(p, count+1);
		}else {
			frequentPatterns.put(p, 1);
		}
	}
	
	void removeFrequentPattern(Triplet t) {
		GraphPattern p = new GraphPattern(t);
		if(frequentPatterns.contains(p)) {
			int count = frequentPatterns.get(p);
			if(count >1)
				frequentPatterns.put(p, count-1);
			else 
				frequentPatterns.remove(p);
		}
	}
	
	public THashMap<GraphPattern, Integer> getFrequentPatterns() {
		return this.frequentPatterns;
	}
	public int getNumberofSubgraphs() {
		return this.numSubgraph;
	}
	
	

}
