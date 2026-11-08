import java.nio.file.*;
import java.util.*;
import java.util.function.*;

public class StudentTests {
    static int failed = 0;
    static Random rnd = new Random(2026);

    public static void main(String[] args) throws Exception {
        fixtures(Path.of("tests", "student"));
        oracles(500);
        scale();
        bench();
        System.out.println(failed == 0 ? "ALL PASSED" : failed + " FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }

    static void check(boolean ok, String label) {
        if (!ok) failed++;
        System.out.println((ok ? "PASS " : "FAIL ") + label);
    }

    @SuppressWarnings("unchecked")
    static Map<String,Object> map(Object o) { return (Map<String,Object>) o; }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object o) { return (List<Object>) o; }

    static long L(Object o) { return (Long) o; }

    static Map<String,Object> req(Object... kv) {
        Map<String,Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    static Map<String,Object> run(Map<String,Object> r) { return map(Json.parse(Json.write(Main.handle(r)))); }

    static String randStr(Random g, int n, String abc) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(abc.charAt(g.nextInt(abc.length())));
        return b.toString();
    }

    static List<Object> occurrences(String t, String p) {
        List<Object> r = new ArrayList<>();
        for (int i = 0; i + p.length() <= t.length(); i++) if (t.startsWith(p, i)) r.add((long) i);
        return r;
    }

    static boolean isSub(String s, String t) {
        int j = 0;
        for (int i = 0; i < t.length() && j < s.length(); i++) if (t.charAt(i) == s.charAt(j)) j++;
        return j == s.length();
    }

    static int common(String s, int i, int j) {
        int h = 0;
        while (i + h < s.length() && j + h < s.length() && s.charAt(i + h) == s.charAt(j + h)) h++;
        return h;
    }

    static void fixtures(Path dir) throws Exception {
        List<String> in = Files.readAllLines(dir.resolve("cases.jsonl")), out = Files.readAllLines(dir.resolve("expected.jsonl"));
        check(in.size() == out.size(), "fixture files have " + in.size() + " requests");
        for (int i = 0; i < in.size(); i++) {
            String actual = Json.write(Main.handle(map(Json.parse(in.get(i)))));
            check(Json.parse(actual).equals(Json.parse(out.get(i))), "fixture " + (i + 1) + ": " + actual);
        }
    }

    static int lcsBrute(String a, String b) {
        int best = 0;
        for (int mask = 0; mask < 1 << a.length(); mask++) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < a.length(); i++) if ((mask >> i & 1) == 1) s.append(a.charAt(i));
            if (isSub(s.toString(), b)) best = Math.max(best, s.length());
        }
        return best;
    }

    static long mergeBrute(List<Long> b) {
        long best = b.size() <= 1 ? 0 : Long.MAX_VALUE;
        for (int i = 0; i + 1 < b.size(); i++) {
            List<Long> c = new ArrayList<>(b);
            long s = c.get(i) + c.remove(i + 1);
            c.set(i, s);
            best = Math.min(best, s + mergeBrute(c));
        }
        return best;
    }

    static long planCost(List<Object> sizes, List<Object> plan) {
        List<long[]> blocks = new ArrayList<>();
        for (int i = 0; i < sizes.size(); i++) blocks.add(new long[]{i, i});
        long cost = 0;
        for (Object o : plan) {
            long l = L(list(o).get(0)), k = L(list(o).get(1)), r = L(list(o).get(2));
            int j = 0;
            while (j + 1 < blocks.size() && !(blocks.get(j)[0] == l && blocks.get(j)[1] == k)) j++;
            if (j + 1 >= blocks.size() || blocks.get(j)[0] != l || blocks.get(j)[1] != k
                    || blocks.get(j + 1)[0] != k + 1 || blocks.get(j + 1)[1] != r) return -1;
            blocks.remove(j + 1);
            blocks.set(j, new long[]{l, r});
            for (long i = l; i <= r; i++) cost += L(sizes.get((int) i));
        }
        return blocks.size() <= 1 && plan.size() == Math.max(0, sizes.size() - 1) ? cost : -1;
    }

    static long setWeight(List<Object> w, List<Object> edges, List<Object> sel) {
        Set<Long> s = new HashSet<>();
        long sum = 0;
        for (Object v : sel) {
            if (L(v) < 0 || L(v) >= w.size() || !s.add(L(v))) return Long.MIN_VALUE;
            sum += L(w.get((int) L(v)));
        }
        for (Object e : edges) if (s.contains(L(list(e).get(0))) && s.contains(L(list(e).get(1)))) return Long.MIN_VALUE;
        return sum;
    }

    static long treeBrute(int n, List<Object> w, List<Object> edges) {
        long best = 0;
        for (int mask = 0; mask < 1 << n; mask++) {
            List<Object> sel = new ArrayList<>();
            for (int v = 0; v < n; v++) if ((mask >> v & 1) == 1) sel.add((long) v);
            best = Math.max(best, setWeight(w, edges, sel));
        }
        return best;
    }

    static boolean suffixBrute(String s, Map<String,Object> y) {
        int n = s.length(), best = 0;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> s.substring(a).compareTo(s.substring(b)));
        List<Object> sa = new ArrayList<>(), lcp = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            sa.add((long) idx[i]);
            lcp.add((long) (i == 0 ? 0 : common(s, idx[i - 1], idx[i])));
        }
        for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) best = Math.max(best, common(s, i, j));
        String rep = (String) y.get("repeated");
        return sa.equals(y.get("sa")) && lcp.equals(y.get("lcp")) && rep.length() == best && (best == 0 || occurrences(s, rep).size() >= 2);
    }

    static boolean sortedWithLcp(String s, Map<String,Object> y) {
        List<Object> sa = list(y.get("sa")), lcp = list(y.get("lcp"));
        int n = s.length(), best = 0;
        if (sa.size() != n || lcp.size() != n || L(lcp.get(0)) != 0) return false;
        boolean[] seen = new boolean[n];
        for (Object o : sa) { if (seen[(int) L(o)]) return false; seen[(int) L(o)] = true; }
        for (int r = 1; r < n; r++) {
            int i = (int) L(sa.get(r - 1)), j = (int) L(sa.get(r)), h = (int) L(lcp.get(r));
            if (common(s, i, j) != h || j + h == n || (i + h < n && s.charAt(i + h) > s.charAt(j + h))) return false;
            best = Math.max(best, h);
        }
        String rep = (String) y.get("repeated");
        return rep.length() == best && (best == 0 || occurrences(s, rep).size() >= 2);
    }

    static void oracles(int trials) {
        boolean lcs = true, merge = true, tree = true, search = true, suffix = true, aho = true;
        for (int t = 0; t < trials; t++) {
            String a = randStr(rnd, rnd.nextInt(11), "abc"), b = randStr(rnd, rnd.nextInt(11), "abc");
            Map<String,Object> y = run(req("task", "lcs", "a", a, "b", b));
            String seq = (String) y.get("sequence");
            int opt = lcsBrute(a, b);
            lcs &= L(y.get("length")) == opt && seq.length() == opt && isSub(seq, a) && isSub(seq, b);

            List<Object> sizes = new ArrayList<>();
            List<Long> blocks = new ArrayList<>();
            for (int i = rnd.nextInt(8); i > 0; i--) { long v = rnd.nextInt(6); sizes.add(v); blocks.add(v); }
            y = run(req("task", "merge", "sizes", sizes));
            merge &= L(y.get("cost")) == mergeBrute(blocks) && planCost(sizes, list(y.get("plan"))) == L(y.get("cost"));

            int n = rnd.nextInt(11);
            List<Object> w = new ArrayList<>(), edges = new ArrayList<>();
            for (int i = 0; i < n; i++) w.add((long) rnd.nextInt(11) - 5);
            for (int i = 1; i < n; i++) {
                long p = rnd.nextInt(i);
                edges.add(rnd.nextBoolean() ? List.of(p, (long) i) : List.of((long) i, p));
            }
            y = run(req("task", "tree", "n", (long) n, "weights", w, "edges", edges));
            tree &= L(y.get("weight")) == treeBrute(n, w, edges) && setWeight(w, edges, list(y.get("selected"))) == L(y.get("weight"));

            String abc = t % 2 == 0 ? "ab" : " aA!";
            String text = randStr(rnd, rnd.nextInt(31), abc), pat = randStr(rnd, rnd.nextInt(5), abc);
            for (String task : new String[]{"naive", "kmp", "rk"})
                search &= run(req("task", task, "text", text, "pattern", pat)).get("positions").equals(occurrences(text, pat));

            String s = randStr(rnd, rnd.nextInt(21), "ab");
            suffix &= suffixBrute(s, run(req("task", "suffix", "text", s)));

            List<Object> pats = new ArrayList<>(), texts = new ArrayList<>(), exp = new ArrayList<>();
            for (int i = rnd.nextInt(6); i > 0; i--) pats.add(randStr(rnd, 1 + rnd.nextInt(3), "ab"));
            for (int i = rnd.nextInt(4); i > 0; i--) texts.add(randStr(rnd, rnd.nextInt(16), "ab"));
            for (Object tx : texts) {
                List<Object> row = new ArrayList<>();
                for (Object p : pats) row.add((long) occurrences((String) tx, (String) p).size());
                exp.add(row);
            }
            aho &= exp.equals(run(req("task", "aho", "patterns", pats, "texts", texts)).get("counts"));
        }
        check(lcs, "oracle lcs: " + trials + " random cases vs subset enumeration, witness is a common subsequence");
        check(merge, "oracle merge: " + trials + " random cases vs all adjacent merge orders, plan replayed");
        check(tree, "oracle tree: " + trials + " random trees vs all subsets, selected set independent");
        check(search, "oracle naive/kmp/rk: " + trials + " random pairs vs startsWith at every boundary");
        check(suffix, "oracle suffix: " + trials + " random texts vs sorted substrings and direct LCP");
        check(aho, "oracle aho: " + trials + " random dictionaries vs counting every start");
    }

    static void timed(String label, BooleanSupplier test) {
        long t0 = System.nanoTime();
        boolean ok = test.getAsBoolean();
        check(ok, label + " (" + (System.nanoTime() - t0) / 1_000_000 + " ms)");
    }

    static void scale() {
        Random g = new Random(7);
        String a = randStr(g, 1000, "abcd");
        timed("scale lcs 1000x1000 identical strings", () -> {
            Map<String,Object> y = run(req("task", "lcs", "a", a, "b", a));
            return L(y.get("length")) == 1000 && a.equals(y.get("sequence"));
        });
        timed("scale merge 200 blocks of 1e9, expected 1544000000000", () -> {
            List<Object> sizes = new ArrayList<>(Collections.nCopies(200, 1_000_000_000L));
            Map<String,Object> y = run(req("task", "merge", "sizes", sizes));
            return L(y.get("cost")) == 1_544_000_000_000L && planCost(sizes, list(y.get("plan"))) == L(y.get("cost"));
        });
        timed("scale tree chain of 100000 vertices of 1e9, expected 5e13", () -> {
            List<Object> w = new ArrayList<>(Collections.nCopies(100000, 1_000_000_000L)), edges = new ArrayList<>();
            for (long i = 0; i + 1 < 100000; i++) edges.add(List.of(i, i + 1));
            Map<String,Object> y = run(req("task", "tree", "n", 100000L, "weights", w, "edges", edges));
            return L(y.get("weight")) == 50_000_000_000_000L && setWeight(w, edges, list(y.get("selected"))) == L(y.get("weight"));
        });
        timed("scale kmp a^200000 / a^100000, 100001 starts", () -> {
            List<Object> p = list(run(req("task", "kmp", "text", "a".repeat(200000), "pattern", "a".repeat(100000))).get("positions"));
            return p.size() == 100001 && L(p.get(0)) == 0 && L(p.get(100000)) == 100000;
        });
        timed("scale rk a^10000 / a^2000, every window is a candidate", () -> {
            List<Object> p = list(run(req("task", "rk", "text", "a".repeat(10000), "pattern", "a".repeat(2000))).get("positions"));
            return p.size() == 8001 && L(p.get(8000)) == 8000;
        });
        timed("scale suffix a^50000", () -> {
            Map<String,Object> y = run(req("task", "suffix", "text", "a".repeat(50000)));
            List<Object> sa = list(y.get("sa")), lcp = list(y.get("lcp"));
            for (int i = 0; i < 50000; i++) if (L(sa.get(i)) != 49999 - i || L(lcp.get(i)) != i) return false;
            return ((String) y.get("repeated")).length() == 49999;
        });
        String rs = randStr(g, 50000, "ab");
        timed("scale suffix random 50000 over ab, adjacent order and LCP verified", () -> sortedWithLcp(rs, run(req("task", "suffix", "text", rs))));
        timed("scale aho dense: a^1..a^446 in 100 texts a^2000", () -> {
            List<Object> pats = new ArrayList<>(), texts = new ArrayList<>(Collections.nCopies(100, "a".repeat(2000)));
            for (int k = 1; k <= 446; k++) pats.add("a".repeat(k));
            List<Object> counts = list(run(req("task", "aho", "patterns", pats, "texts", texts)).get("counts"));
            for (Object row : counts) for (int k = 1; k <= 446; k++) if (L(list(row).get(k - 1)) != 2001 - k) return false;
            return counts.size() == 100;
        });
        timed("scale aho wide: 10000 patterns of length 10, 100 texts of 2000", () -> {
            List<Object> pats = new ArrayList<>(), texts = new ArrayList<>();
            for (int i = 0; i < 10000; i++) pats.add(randStr(g, 10, "ab"));
            for (int i = 0; i < 100; i++) texts.add(randStr(g, 2000, "ab"));
            List<Object> counts = list(run(req("task", "aho", "patterns", pats, "texts", texts)).get("counts"));
            for (int t = 0; t < 100; t++) {
                Map<String,Long> windows = new HashMap<>();
                String tx = (String) texts.get(t);
                for (int i = 0; i + 10 <= tx.length(); i++) windows.merge(tx.substring(i, i + 10), 1L, Long::sum);
                for (int i = 0; i < 10000; i++)
                    if (L(list(counts.get(t)).get(i)) != windows.getOrDefault((String) pats.get(i), 0L)) return false;
            }
            return true;
        });
    }

    static void bench() {
        Map<String,Function<Map<String,Object>,Map<String,Object>>> algs = new LinkedHashMap<>();
        algs.put("naive", Algorithms::naive);
        algs.put("kmp", Algorithms::kmp);
        algs.put("rk", Algorithms::rk);
        List<String[]> cases = new ArrayList<>();
        for (String kind : new String[]{"random", "repetitive"})
            for (int n : new int[]{1000, 2000, 3000}) {
                int m = n / 10;
                Random g = new Random(n);
                if (kind.equals("random")) {
                    String text = randStr(g, n, "abcd");
                    int at = g.nextInt(n - m + 1);
                    cases.add(new String[]{kind, text, text.substring(at, at + m), "Random(" + n + "), abcd, window of text"});
                } else cases.add(new String[]{kind, "a".repeat(n), "a".repeat(m), "a^n, a^m"});
            }
        for (String[] c : cases)
            for (Map.Entry<String,Function<Map<String,Object>,Map<String,Object>>> e : algs.entrySet()) {
                Map<String,Object> r = req("text", c[1], "pattern", c[2]);
                check(map(Json.parse(Json.write(e.getValue().apply(r)))).get("positions").equals(occurrences(c[1], c[2])),
                        "bench output " + c[0] + " n=" + c[1].length() + " " + e.getKey());
                for (int i = 0; i < 500; i++) e.getValue().apply(r);
            }
        System.out.println();
        System.out.println("| Case | n | m | Seed/generator | Algorithm | Run 1 | Run 2 | Run 3 | Median | Correct outputs checked |");
        System.out.println("|---|---:|---:|---|---|---:|---:|---:|---:|---|");
        for (String[] c : cases)
            for (Map.Entry<String,Function<Map<String,Object>,Map<String,Object>>> e : algs.entrySet()) {
                Map<String,Object> r = req("text", c[1], "pattern", c[2]);
                double[] runs = new double[3];
                for (int k = 0; k < 3; k++) {
                    long t0 = System.nanoTime();
                    for (int i = 0; i < 200; i++) e.getValue().apply(r);
                    runs[k] = (System.nanoTime() - t0) / 200 / 1000.0;
                }
                double[] sorted = runs.clone();
                Arrays.sort(sorted);
                System.out.printf(Locale.ROOT, "| %s | %d | %d | %s | %s | %.1f us | %.1f us | %.1f us | %.1f us | yes, z=%d |%n",
                        c[0], c[1].length(), c[2].length(), c[3], e.getKey(), runs[0], runs[1], runs[2], sorted[1], occurrences(c[1], c[2]).size());
            }
    }
}
