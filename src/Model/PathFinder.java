package Model;

import Implementation.Graph;
import Implementation.HeapPriorityQueue;

import java.util.*;

/**
 * Pathfinder - Implements Dijkstra's shortest-path algorithm using
 * our custom HeapPriorityQueue ADT.
 * Used for:
 *   - Path hint skill (show shortest path to exit)
 *   - AI enemy pathfinding
 */
public class Pathfinder {

    /** Entry in the priority queue for Dijkstra's algorithm. */
    private static class DijkstraEntry implements Comparable<DijkstraEntry> {
        final String vertex;
        final double distance;

        DijkstraEntry(String vertex, double distance) {
            this.vertex = vertex;
            this.distance = distance;
        }

        @Override
        public int compareTo(DijkstraEntry other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    /**
     * Find the shortest path from source to target using Dijkstra's algorithm.
     *
     * @param graph  the maze graph
     * @param source start vertex key
     * @param target end vertex key
     * @return list of vertex keys forming the shortest path, or empty if none
     */
    public static List<String> findShortestPath(Graph graph, String source, String target) {
        // Distance map
        Map<String, Double> dist = new HashMap<>();
        // Previous vertex map (for path reconstruction)
        Map<String, String> prev = new HashMap<>();
        // Visited set
        Set<String> visited = new HashSet<>();

        // Initialize distances to infinity
        for (String v : graph.getAllVertices()) {
            dist.put(v, Double.MAX_VALUE);
        }
        dist.put(source, 0.0);

        // Use our custom HeapPriorityQueue ADT
        HeapPriorityQueue<DijkstraEntry> pq = new HeapPriorityQueue<>();
        pq.enqueue(new DijkstraEntry(source, 0.0));

        while (!pq.isEmpty()) {
            DijkstraEntry current = pq.dequeue();
            String u = current.vertex;

            if (visited.contains(u)) continue;
            visited.add(u);

            // Found target - stop early
            if (u.equals(target)) break;

            // Relax edges
            for (String neighbor : graph.getNeighbors(u)) {
                if (visited.contains(neighbor)) continue;

                double newDist = dist.get(u) + graph.getEdgeWeight(u, neighbor);
                if (newDist < dist.get(neighbor)) {
                    dist.put(neighbor, newDist);
                    prev.put(neighbor, u);
                    pq.enqueue(new DijkstraEntry(neighbor, newDist));
                }
            }
        }

        // Reconstruct path
        return reconstructPath(prev, source, target);
    }

    /** Reconstruct the path from prev map. */
    private static List<String> reconstructPath(Map<String, String> prev, String source, String target) {
        List<String> path = new ArrayList<>();
        String current = target;

        if (!prev.containsKey(target) && !source.equals(target)) {
            return path; // No path found
        }

        while (current != null) {
            path.add(0, current);
            if (current.equals(source)) break;
            current = prev.get(current);
        }

        return path;
    }

    /**
     * BFS to find reachable cells within a given distance (for vision).
     */
    public static Set<String> getReachableWithin(MazeGraph graph, String source, int maxSteps) {
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> distMap = new HashMap<>();

        queue.add(source);
        distMap.put(source, 0);
        reachable.add(source);

        while (!queue.isEmpty()) {
            String u = queue.poll();
            int d = distMap.get(u);
            if (d >= maxSteps) continue;

            for (String neighbor : graph.getNeighbors(u)) {
                if (!distMap.containsKey(neighbor)) {
                    distMap.put(neighbor, d + 1);
                    reachable.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return reachable;
    }
}
