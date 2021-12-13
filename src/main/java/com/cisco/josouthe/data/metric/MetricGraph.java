package com.cisco.josouthe.data.metric;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/*
John Southerland: quite proud of this little boy. It reduces 700K metric names to query the controller into 1K metric names with wildcards, on large applications
We build a graph of the metric name split by | and then count references on edges to teh vertex, if we find a bloom, where
a vertex has the same number of left and right vertex references, this is a location we can wildcard, and remove all the nodes in between

this most likely isn't fool proof, i didn't do a proof on this, it just seems to work pretty well. will need to confirm data loads are correct
https://en.wikipedia.org/wiki/Deterministic_acyclic_finite_state_automaton
will most likely migrate to this as my primary data structure, since the ArrayList of ApplicationMetrics is blowing up my limited heap 1GB
 */
public class MetricGraph {
    private static final Logger logger = LogManager.getFormatterLogger();

    private ArrayList<Edge> edges;
    private ArrayList<Vertex> startingVertices;
    private TreeMap<String, Vertex> vertices;
    private long originalSize, newSize, countOfMetricsAdded;
    private Set<String> newAppMetricsStrings = null;

    public MetricGraph(List<String> metrics) {
        edges = new ArrayList<>();
        vertices = new TreeMap<>();
        startingVertices = new ArrayList<>();
        countOfMetricsAdded=0;
        if( metrics != null )
            for( String metricName : metrics)
                addMetricName(metricName);
    }

    public long size() { return countOfMetricsAdded; }

    public Set<String> getUniqueCompressedMetricNames() {
        if( newAppMetricsStrings != null ) return newAppMetricsStrings;
        newAppMetricsStrings = new HashSet<>();
        for( Vertex vertex : vertices.values() ) {                          //O(n^2) or O(n!) lol?!?!
            if( vertex.isFinal() || vertex.isInitial() || vertex.value.contains("*") ) continue;
            int rCnt=0, lCnt=0;
            for( Edge edge : edges) {
                if( edge.lVertex == vertex) lCnt++;
                if( edge.rVertex == vertex) rCnt++;
            }
            if( lCnt == rCnt ) {
                logger.trace("Found Bloom Vertex %s references r: %d l: %d", vertex, rCnt, lCnt);
                newAppMetricsStrings.add( vertex.printWithWildcard() );
            }
        }
        return newAppMetricsStrings;
    }

    public int addMetricNames( String ... metricNames ) {
        int counter=0;
        for( String metricName : metricNames ) {
            addMetricName(metricName);
            counter++;
        }
        return counter;
    }

    public void addMetricName(String metricName) {
        Vertex leftVertex = null;
        int position = 0;
        for( String word : metricName.split("\\|") ) {
            Vertex vertex = vertices.get(word+String.valueOf(position));
            if( vertex == null ) vertex = new Vertex(word, position, metricName);
            if( leftVertex == null ) {
                startingVertices.add(vertex);
            } else {
                edges.add( new Edge(leftVertex,vertex) );
                leftVertex.addRightVertex(vertex);
                vertex.addLeftVertex(leftVertex);
            }
            vertices.put(word+String.valueOf(position),vertex);
            position++;
            leftVertex=vertex;
        }
        countOfMetricsAdded++;
        this.newAppMetricsStrings=null;
    }

    public int addMetricNames(List<String> metricPaths) {
        return addMetricNames( metricPaths.toArray(new String[0]));
    }

    private class Edge {
        public Vertex lVertex, rVertex;

        public Edge(Vertex leftVertex, Vertex vertex) {
            this.lVertex=leftVertex;
            this.rVertex=vertex;
        }
        public boolean isLeftOf( Vertex v ){ return v == rVertex; }
        public boolean isRightOf( Vertex v ) { return v == lVertex; }
        public boolean isSource() { return lVertex == null; }
    }

    private class Vertex implements Comparable{
        public String value, original;
        public int position;
        private ArrayList<Vertex> rEdges, lEdges;

        public Vertex(String word, int position, String original) {
            this.value=word;
            this.original=original;
            this.position=position;
            rEdges = new ArrayList<>();
            lEdges = new ArrayList<>();
        }

        public String toString() { return String.format("'%s' of [%d]'%s'",value, position, original); }
        public String printWithWildcard() {
            StringBuilder sb = new StringBuilder();
            String[] words = original.split("\\|");
            for( int i=0; i< words.length; i++ ) {
                if( i == position ) {
                    sb.append("*");
                } else {
                    sb.append(words[i]);
                }
                if( i+1 < words.length ) sb.append("|");
            }
            return sb.toString();
        }
        public void addRightVertex( Vertex v ) { this.rEdges.add(v); }
        public void addLeftVertex( Vertex v ) { this.lEdges.add(v); }
        public boolean isFinal() { return rEdges.size() == 0;}
        public boolean isInitial() { return lEdges.size() == 0;}

        public boolean isBloomCandidate() {
            if( isFinal() ) return false;
            if( rEdges.size() > 1 ) {
                for( Vertex rightVertex : rEdges)
                    if( rightVertex.isFinal() ) return false;
            }
            return true;
        }

        /**
         * Compares this object with the specified object for order.  Returns a
         * negative integer, zero, or a positive integer as this object is less
         * than, equal to, or greater than the specified object.
         *
         * <p>The implementor must ensure
         * {@code sgn(x.compareTo(y)) == -sgn(y.compareTo(x))}
         * for all {@code x} and {@code y}.  (This
         * implies that {@code x.compareTo(y)} must throw an exception iff
         * {@code y.compareTo(x)} throws an exception.)
         *
         * <p>The implementor must also ensure that the relation is transitive:
         * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
         * {@code x.compareTo(z) > 0}.
         *
         * <p>Finally, the implementor must ensure that {@code x.compareTo(y)==0}
         * implies that {@code sgn(x.compareTo(z)) == sgn(y.compareTo(z))}, for
         * all {@code z}.
         *
         * <p>It is strongly recommended, but <i>not</i> strictly required that
         * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
         * class that implements the {@code Comparable} interface and violates
         * this condition should clearly indicate this fact.  The recommended
         * language is "Note: this class has a natural ordering that is
         * inconsistent with equals."
         *
         * <p>In the foregoing description, the notation
         * {@code sgn(}<i>expression</i>{@code )} designates the mathematical
         * <i>signum</i> function, which is defined to return one of {@code -1},
         * {@code 0}, or {@code 1} according to whether the value of
         * <i>expression</i> is negative, zero, or positive, respectively.
         *
         * @param other the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object
         * is less than, equal to, or greater than the specified object.
         * @throws NullPointerException if the specified object is null
         * @throws ClassCastException   if the specified object's type prevents it
         *                              from being compared to this object.
         */
        @Override
        public int compareTo(Object other) {
            if( other == null ) return 1;
            if( other instanceof Vertex ) {
                Vertex otherV = (Vertex) other;
                if (this.position == otherV.position && this.value.equals(otherV.value)) return 0;
                if (this.position < otherV.position) return -1;
                return 1;
            }
            return -1;
        }
    }

    public static void main( String ... args ) throws Exception{
        ArrayList<String> metrics = new ArrayList<>();
        BufferedReader in = new BufferedReader( new FileReader( args[0] ));
        String inLine;
        while( (inLine = in.readLine()) != null) {
            metrics.add( inLine );
        }
        System.out.println(String.format("Read %d lines from %s",metrics.size(), args[0]));
        MetricGraph graph = new MetricGraph(metrics);
        for( String metricName : graph.getUniqueCompressedMetricNames()) {
            System.out.println(metricName);
        }
    }
}
