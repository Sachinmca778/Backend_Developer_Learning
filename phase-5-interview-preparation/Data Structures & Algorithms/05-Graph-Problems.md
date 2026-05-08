# Graph Problems

## Status: Not Started

---

## Table of Contents

1. [Graph Representation](#graph-representation)
2. [BFS](#bfs)
3. [DFS](#dfs)
4. [Topological Sort — Kahn's BFS](#topological-sort--kahns-bfs)
5. [Topological Sort — DFS](#topological-sort--dfs)
6. [Cycle Detection — Directed](#cycle-detection--directed)
7. [Cycle Detection — Undirected](#cycle-detection--undirected)
8. [Union-Find (DSU)](#union-find-dsu)
9. [Number of Islands](#number-of-islands)
10. [Dijkstra's Algorithm](#dijkstras-algorithm)
11. [Bellman-Ford](#bellman-ford)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Graph Representation

### Adjacency list (most common)

```java
Map<Integer, List<Integer>> graph = new HashMap<>();
// or
List<List<Integer>> graph = new ArrayList<>();
for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
```

### Adjacency matrix

```java
int[][] m = new int[n][n];     // 1 = edge, 0 = no edge
```

### When?

| | Adj list | Adj matrix |
|--|----------|-----------|
| Sparse | ✅ | ❌ (wasted space) |
| Dense | ❌ | ✅ |
| Edge lookup O(1) | ❌ | ✅ |
| Space | O(V+E) | O(V²) |

→ Default: **adjacency list** for most problems.

### Weighted edge

```java
class Edge { int to, weight; }
List<List<Edge>> graph = ...;
```

---

## BFS

### Template — graph (adj list)

```java
void bfs(int start, List<List<Integer>> g) {
    Queue<Integer> q = new ArrayDeque<>();
    boolean[] visited = new boolean[g.size()];
    q.offer(start);
    visited[start] = true;
    
    while (!q.isEmpty()) {
        int curr = q.poll();
        for (int nei : g.get(curr)) {
            if (!visited[nei]) {
                visited[nei] = true;
                q.offer(nei);
            }
        }
    }
}
```

→ **Shortest path in unweighted** graph.

### Template — grid (matrix)

```java
int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};

void bfs(int[][] grid, int sr, int sc) {
    int m = grid.length, n = grid[0].length;
    boolean[][] visited = new boolean[m][n];
    Queue<int[]> q = new ArrayDeque<>();
    q.offer(new int[]{sr, sc});
    visited[sr][sc] = true;
    
    while (!q.isEmpty()) {
        int[] curr = q.poll();
        for (int[] d : dirs) {
            int r = curr[0] + d[0], c = curr[1] + d[1];
            if (r >= 0 && r < m && c >= 0 && c < n && !visited[r][c] && grid[r][c] == 1) {
                visited[r][c] = true;
                q.offer(new int[]{r, c});
            }
        }
    }
}
```

### Multi-source BFS

Add all sources to queue initially → "rotting oranges" / "walls and gates".

---

## DFS

### Recursive

```java
void dfs(int u, List<List<Integer>> g, boolean[] visited) {
    visited[u] = true;
    for (int v : g.get(u)) {
        if (!visited[v]) dfs(v, g, visited);
    }
}
```

### Iterative (stack)

```java
void dfs(int start, List<List<Integer>> g) {
    Deque<Integer> stack = new ArrayDeque<>();
    boolean[] visited = new boolean[g.size()];
    stack.push(start);
    
    while (!stack.isEmpty()) {
        int u = stack.pop();
        if (visited[u]) continue;
        visited[u] = true;
        for (int v : g.get(u)) if (!visited[v]) stack.push(v);
    }
}
```

→ Stack overflow possible for deep graphs — iterative safer.

---

## Topological Sort — Kahn's BFS

**Pre-condition:** Directed Acyclic Graph (DAG).

```java
List<Integer> topoSort(int n, List<List<Integer>> g) {
    int[] indeg = new int[n];
    for (List<Integer> adj : g) for (int v : adj) indeg[v]++;
    
    Queue<Integer> q = new ArrayDeque<>();
    for (int i = 0; i < n; i++) if (indeg[i] == 0) q.offer(i);
    
    List<Integer> order = new ArrayList<>();
    while (!q.isEmpty()) {
        int u = q.poll();
        order.add(u);
        for (int v : g.get(u)) {
            if (--indeg[v] == 0) q.offer(v);
        }
    }
    
    if (order.size() != n) return Collections.emptyList();   // cycle exists
    return order;
}
```

→ Time **O(V + E)**.

### Use case

Course schedule, build-order, dependency resolution.

---

## Topological Sort — DFS

```java
List<Integer> topoSort(int n, List<List<Integer>> g) {
    boolean[] visited = new boolean[n];
    Deque<Integer> stack = new ArrayDeque<>();
    
    for (int i = 0; i < n; i++) {
        if (!visited[i]) dfs(i, g, visited, stack);
    }
    
    List<Integer> order = new ArrayList<>();
    while (!stack.isEmpty()) order.add(stack.pop());
    return order;
}

void dfs(int u, List<List<Integer>> g, boolean[] visited, Deque<Integer> stack) {
    visited[u] = true;
    for (int v : g.get(u)) if (!visited[v]) dfs(v, g, visited, stack);
    stack.push(u);     // push after completing
}
```

→ Need cycle detection separately for completeness.

---

## Cycle Detection — Directed

### Three-color DFS

```java
int[] color;   // 0 = white, 1 = gray (visiting), 2 = black (done)

boolean hasCycle(int n, List<List<Integer>> g) {
    color = new int[n];
    for (int i = 0; i < n; i++) if (color[i] == 0 && dfs(i, g)) return true;
    return false;
}

boolean dfs(int u, List<List<Integer>> g) {
    color[u] = 1;
    for (int v : g.get(u)) {
        if (color[v] == 1) return true;       // back edge → cycle
        if (color[v] == 0 && dfs(v, g)) return true;
    }
    color[u] = 2;
    return false;
}
```

### Alternative — Kahn's: if order size < n → cycle exists.

---

## Cycle Detection — Undirected

### DFS + parent tracking

```java
boolean hasCycle(int n, List<List<Integer>> g) {
    boolean[] visited = new boolean[n];
    for (int i = 0; i < n; i++) {
        if (!visited[i] && dfs(i, -1, g, visited)) return true;
    }
    return false;
}

boolean dfs(int u, int parent, List<List<Integer>> g, boolean[] visited) {
    visited[u] = true;
    for (int v : g.get(u)) {
        if (!visited[v]) {
            if (dfs(v, u, g, visited)) return true;
        } else if (v != parent) {
            return true;     // visited & not parent → cycle
        }
    }
    return false;
}
```

### Union-Find — alternative

If two nodes already in same component when adding edge → cycle.

---

## Union-Find (DSU)

**Disjoint Set Union** — O(α(n)) ≈ O(1) per op with optimizations.

### With path compression + union by rank

```java
class UnionFind {
    int[] parent, rank;
    int components;
    
    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        components = n;
    }
    
    public int find(int x) {
        if (parent[x] != x) parent[x] = find(parent[x]);   // path compression
        return parent[x];
    }
    
    public boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        
        if (rank[ra] < rank[rb]) { parent[ra] = rb; }
        else if (rank[ra] > rank[rb]) { parent[rb] = ra; }
        else { parent[rb] = ra; rank[ra]++; }
        
        components--;
        return true;
    }
    
    public boolean connected(int a, int b) { return find(a) == find(b); }
}
```

### Use cases

- Number of provinces / connected components
- MST (Kruskal's)
- Cycle detection in undirected graph
- Account merge

---

## Number of Islands

**Q:** Count connected components of '1's in grid.

### DFS

```java
int numIslands(char[][] grid) {
    int m = grid.length, n = grid[0].length, count = 0;
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (grid[i][j] == '1') {
                dfs(grid, i, j);
                count++;
            }
        }
    }
    return count;
}

void dfs(char[][] g, int r, int c) {
    if (r < 0 || r >= g.length || c < 0 || c >= g[0].length || g[r][c] != '1') return;
    g[r][c] = '0';   // mark visited
    dfs(g, r-1, c); dfs(g, r+1, c); dfs(g, r, c-1); dfs(g, r, c+1);
}
```

→ Time **O(m × n)**.

### BFS / Union-Find variants — same complexity.

---

## Dijkstra's Algorithm

**Single-source shortest path** with **non-negative** weights.

```java
int[] dijkstra(int n, List<List<int[]>> g, int src) {
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[src] = 0;
    
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[1] - b[1]);
    pq.offer(new int[]{src, 0});
    
    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        int u = curr[0], d = curr[1];
        if (d > dist[u]) continue;     // stale entry
        
        for (int[] e : g.get(u)) {
            int v = e[0], w = e[1];
            if (dist[u] + w < dist[v]) {
                dist[v] = dist[u] + w;
                pq.offer(new int[]{v, dist[v]});
            }
        }
    }
    return dist;
}
```

→ Time **O((V + E) log V)** with binary heap.

→ Java's `PriorityQueue` doesn't support decrease-key — re-insert + skip stale.

### Failure with negatives

Negative edge → Dijkstra wrong. Use Bellman-Ford.

---

## Bellman-Ford

**Handles negative edges; detects negative cycles.**

```java
int[] bellmanFord(int n, int[][] edges, int src) {
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[src] = 0;
    
    for (int i = 0; i < n - 1; i++) {
        for (int[] e : edges) {
            int u = e[0], v = e[1], w = e[2];
            if (dist[u] != Integer.MAX_VALUE && dist[u] + w < dist[v]) {
                dist[v] = dist[u] + w;
            }
        }
    }
    
    // Detect negative cycle
    for (int[] e : edges) {
        if (dist[e[0]] != Integer.MAX_VALUE && dist[e[0]] + e[2] < dist[e[1]]) {
            throw new IllegalStateException("Negative cycle");
        }
    }
    return dist;
}
```

→ Time **O(V × E)**.

### Comparison

| | Dijkstra | Bellman-Ford |
|--|---------|--------------|
| Negative weights | ❌ | ✅ |
| Negative cycle detection | — | ✅ |
| Time | O((V+E) log V) | O(VE) |

---

## Common Output Traps

### Q1. Forgetting `visited` → infinite loop

Especially in cyclic graphs.

### Q2. Stack overflow on deep DFS

```java
// 10⁵ nodes chain → recursion stack overflow
// Use iterative DFS
```

### Q3. Dijkstra with negative edge

Wrong answer; use Bellman-Ford.

### Q4. Marking visited at wrong time (BFS)

```java
// ❌ Mark when polling — same node added multiple times
// ✅ Mark when offering — visited check before offer
```

### Q5. PriorityQueue stale entries

```java
if (d > dist[u]) continue;   // skip outdated
```

---

## Pitfalls

1. **Recursion depth** for sparse path → iterative DFS.
2. **`HashSet<int[]>`** doesn't dedupe — use encoded `int` (`r * cols + c`).
3. **Graph from edges** — build adj list once; don't recompute.
4. **Bidirectional edges** — add both directions for undirected.
5. **Self-loops** — skip in DFS; explicit handling in cycle detection.
6. **Disconnected graph** — outer loop over all nodes.
7. **Large weights** — `int` overflow; use `long` for distances.
8. **Modifying input grid** — sometimes acceptable; clarify.
9. **Multi-source BFS** — initialize queue with all sources.
10. **`indegree` not decremented** in Kahn's — wrong topo order.

---

## Cheat Sheet

| Algorithm | Time | Space | Use |
|-----------|------|-------|-----|
| BFS | O(V+E) | O(V) | Shortest path unweighted |
| DFS | O(V+E) | O(V) | All paths / connectivity |
| Topo sort | O(V+E) | O(V) | DAG ordering |
| Union-Find | O(α(n)) | O(n) | Connectivity |
| Dijkstra | O((V+E) log V) | O(V) | Shortest, non-neg |
| Bellman-Ford | O(VE) | O(V) | Negative weights |
| Floyd-Warshall | O(V³) | O(V²) | All-pairs |

| Problem | Best DS |
|---------|---------|
| Connected components | Union-Find / BFS |
| Course schedule | Topo sort |
| Cheapest flights K stops | Bellman-Ford (limited) |
| Network delay | Dijkstra |
| Word ladder | BFS |
| Number of islands | DFS / BFS / UF |

---

## Practice (LeetCode)

| # | Problem |
|---|---------|
| 200 | Number of Islands |
| 207 | Course Schedule |
| 210 | Course Schedule II |
| 261 | Graph Valid Tree |
| 269 | Alien Dictionary |
| 323 | Connected Components |
| 547 | Number of Provinces (UF) |
| 684 | Redundant Connection (UF) |
| 743 | Network Delay Time (Dijkstra) |
| 787 | Cheapest Flights K Stops (Bellman-Ford) |
| 994 | Rotting Oranges (BFS) |
| 1162 | As Far From Land |
| 127 | Word Ladder |
| 130 | Surrounded Regions |
| 133 | Clone Graph |
| 417 | Pacific Atlantic Water Flow |
| 1971 | Find If Path Exists in Graph |
