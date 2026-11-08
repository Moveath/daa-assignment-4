import java.util.*;

public final class Algorithms {
    @SuppressWarnings("unchecked")
    static List<Object> list(Object o) { return (List<Object>) o; }

    static int num(Object o) { return ((Long) o).intValue(); }

    static Map<String,Object> lcs(Map<String,Object> input) {
        String a = (String) input.get("a"), b = (String) input.get("b");
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++)
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1) ? dp[i - 1][j - 1] + 1 : Math.max(dp[i - 1][j], dp[i][j - 1]);
        StringBuilder seq = new StringBuilder();
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) { seq.append(a.charAt(i - 1)); i--; j--; }
            else if (dp[i - 1][j] >= dp[i][j - 1]) i--;
            else j--;
        }
        return Map.of("length", dp[n][m], "sequence", seq.reverse().toString());
    }

    static Map<String,Object> merge(Map<String,Object> input) {
        List<Object> sizes = list(input.get("sizes"));
        int n = sizes.size();
        List<int[]> plan = new ArrayList<>();
        if (n <= 1) return Map.of("cost", 0L, "plan", plan);
        long[] pre = new long[n + 1];
        for (int i = 0; i < n; i++) pre[i + 1] = pre[i] + (Long) sizes.get(i);
        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];
        for (int len = 2; len <= n; len++)
            for (int l = 0, r = len - 1; r < n; l++, r++) {
                dp[l][r] = Long.MAX_VALUE;
                for (int k = l; k < r; k++)
                    if (dp[l][k] + dp[k + 1][r] < dp[l][r]) { dp[l][r] = dp[l][k] + dp[k + 1][r]; split[l][r] = k; }
                dp[l][r] += pre[r + 1] - pre[l];
            }
        addPlan(0, n - 1, split, plan);
        return Map.of("cost", dp[0][n - 1], "plan", plan);
    }

    static void addPlan(int l, int r, int[][] split, List<int[]> plan) {
        if (l == r) return;
        int k = split[l][r];
        addPlan(l, k, split, plan);
        addPlan(k + 1, r, split, plan);
        plan.add(new int[]{l, k, r});
    }

    static Map<String,Object> tree(Map<String,Object> input) {
        int n = num(input.get("n"));
        List<Integer> selected = new ArrayList<>();
        if (n == 0) return Map.of("weight", 0L, "selected", selected);
        List<Object> weights = list(input.get("weights"));
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (Object e : list(input.get("edges"))) {
            int u = num(list(e).get(0)), v = num(list(e).get(1));
            adj.get(u).add(v);
            adj.get(v).add(u);
        }
        int[] order = new int[n], parent = new int[n], stack = new int[n];
        boolean[] seen = new boolean[n];
        int top = 0, cnt = 0;
        parent[0] = -1;
        seen[0] = true;
        stack[top++] = 0;
        while (top > 0) {
            int v = stack[--top];
            order[cnt++] = v;
            for (int u : adj.get(v))
                if (!seen[u]) { seen[u] = true; parent[u] = v; stack[top++] = u; }
        }
        long[] take = new long[n], skip = new long[n];
        for (int i = n - 1; i >= 0; i--) {
            int v = order[i], p = parent[v];
            take[v] += (Long) weights.get(v);
            if (p >= 0) {
                take[p] += skip[v];
                skip[p] += Math.max(take[v], skip[v]);
            }
        }
        boolean[] in = new boolean[n];
        for (int i = 0; i < n; i++) {
            int v = order[i], p = parent[v];
            in[v] = (p < 0 || !in[p]) && take[v] > skip[v];
            if (in[v]) selected.add(v);
        }
        return Map.of("weight", Math.max(take[0], skip[0]), "selected", selected);
    }

    static Map<String,Object> naive(Map<String,Object> input) {
        String t = (String) input.get("text"), p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        List<Integer> pos = new ArrayList<>();
        for (int i = 0; i + m <= n; i++) {
            int j = 0;
            while (j < m && t.charAt(i + j) == p.charAt(j)) j++;
            if (j == m) pos.add(i);
        }
        return Map.of("positions", pos);
    }

    static Map<String,Object> kmp(Map<String,Object> input) {
        String t = (String) input.get("text"), p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        List<Integer> pos = new ArrayList<>();
        if (m == 0) {
            for (int i = 0; i <= n; i++) pos.add(i);
            return Map.of("positions", pos);
        }
        int[] pi = new int[m];
        for (int i = 1, k = 0; i < m; i++) {
            while (k > 0 && p.charAt(i) != p.charAt(k)) k = pi[k - 1];
            if (p.charAt(i) == p.charAt(k)) k++;
            pi[i] = k;
        }
        for (int i = 0, k = 0; i < n; i++) {
            while (k > 0 && t.charAt(i) != p.charAt(k)) k = pi[k - 1];
            if (t.charAt(i) == p.charAt(k)) k++;
            if (k == m) { pos.add(i - m + 1); k = pi[k - 1]; }
        }
        return Map.of("positions", pos);
    }

    static Map<String,Object> rk(Map<String,Object> input) {
        final long B = 31, M = 101;
        String t = (String) input.get("text"), p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        List<Integer> pos = new ArrayList<>();
        if (m == 0) {
            for (int i = 0; i <= n; i++) pos.add(i);
            return Map.of("positions", pos);
        }
        if (m > n) return Map.of("positions", pos);
        long high = 1, hp = 0, ht = 0;
        for (int i = 0; i < m - 1; i++) high = high * B % M;
        for (int i = 0; i < m; i++) {
            hp = (hp * B + p.charAt(i)) % M;
            ht = (ht * B + t.charAt(i)) % M;
        }
        for (int i = 0; ; i++) {
            if (hp == ht) {
                int j = 0;
                while (j < m && t.charAt(i + j) == p.charAt(j)) j++;
                if (j == m) pos.add(i);
            }
            if (i + m == n) break;
            ht = ((ht - t.charAt(i) * high) % M + M) % M;
            ht = (ht * B + t.charAt(i + m)) % M;
        }
        return Map.of("positions", pos);
    }

    static Map<String,Object> suffix(Map<String,Object> input) {
        String s = (String) input.get("text");
        int n = s.length();
        int[] sa = new int[n], lcp = new int[n], rank = new int[n], tmp = new int[n];
        if (n == 0) return Map.of("sa", sa, "lcp", lcp, "repeated", "");
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) { idx[i] = i; rank[i] = s.charAt(i); }
        for (int k = 1; ; k <<= 1) {
            final int step = k;
            Comparator<Integer> cmp = (x, y) -> rank[x] != rank[y] ? Integer.compare(rank[x], rank[y])
                    : Integer.compare(x + step < n ? rank[x + step] : -1, y + step < n ? rank[y + step] : -1);
            Arrays.sort(idx, cmp);
            tmp[idx[0]] = 0;
            for (int i = 1; i < n; i++) tmp[idx[i]] = tmp[idx[i - 1]] + (cmp.compare(idx[i - 1], idx[i]) < 0 ? 1 : 0);
            System.arraycopy(tmp, 0, rank, 0, n);
            if (rank[idx[n - 1]] == n - 1) break;
        }
        for (int i = 0; i < n; i++) { sa[i] = idx[i]; rank[sa[i]] = i; }
        int best = 0, at = 0;
        for (int i = 0, h = 0; i < n; i++) {
            if (rank[i] == 0) { h = 0; continue; }
            int j = sa[rank[i] - 1];
            while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
            lcp[rank[i]] = h;
            if (h > best) { best = h; at = i; }
            if (h > 0) h--;
        }
        return Map.of("sa", sa, "lcp", lcp, "repeated", s.substring(at, at + best));
    }

    static Map<String,Object> aho(Map<String,Object> input) {
        final int A = 95;
        List<Object> patterns = list(input.get("patterns")), texts = list(input.get("texts"));
        int p = patterns.size(), total = 1;
        for (Object o : patterns) total += ((String) o).length();
        int[] go = new int[total * A], fail = new int[total], term = new int[p];
        int states = 1;
        for (int i = 0; i < p; i++) {
            String w = (String) patterns.get(i);
            int v = 0;
            for (int j = 0; j < w.length(); j++) {
                int c = v * A + w.charAt(j) - 32;
                if (go[c] == 0) go[c] = states++;
                v = go[c];
            }
            term[i] = v;
        }
        int[] order = new int[states];
        for (int head = 0, tail = 1; head < tail; head++) {
            int u = order[head];
            for (int c = 0; c < A; c++) {
                int v = go[u * A + c];
                int f = u == 0 ? 0 : go[fail[u] * A + c];
                if (v != 0) { fail[v] = f; order[tail++] = v; }
                else go[u * A + c] = f;
            }
        }
        List<long[]> counts = new ArrayList<>();
        long[] visits = new long[states];
        for (Object o : texts) {
            String t = (String) o;
            Arrays.fill(visits, 0);
            int v = 0;
            for (int j = 0; j < t.length(); j++) {
                v = go[v * A + t.charAt(j) - 32];
                visits[v]++;
            }
            for (int i = states - 1; i > 0; i--) visits[fail[order[i]]] += visits[order[i]];
            long[] row = new long[p];
            for (int i = 0; i < p; i++) row[i] = visits[term[i]];
            counts.add(row);
        }
        return Map.of("counts", counts);
    }
}
