# Assignment 4 — analysis

## Environment and commands

- Language: Java (`submission.json`), compiled with `--release 17` by JDK 25.0.1 (Oracle HotSpot 25.0.1+8-LTS).
- Windows 11, Intel Core i7-13650HX , 16 GB RAM, Python 3.14.3 for the launcher.

Run from the repository root (on Windows `py -3` may replace `python`):

```sh
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input tests/student/cases.jsonl
javac --release 17 -encoding UTF-8 -d Java/build Java/source/*.java tests/student/StudentTests.java
java -Xmx384m -cp Java/build StudentTests > tests/student/results.txt
```


## Algorithm explanations

### lcs

- State: `dp[i][j]` is the LCS length of prefixes `a[0..i)` and `b[0..j)`.
- Base: `dp[0][j] = dp[i][0] = 0` (an empty prefix has no common subsequence).
- Transition: if `a[i-1] == b[j-1]` then `dp[i-1][j-1] + 1`, else `max(dp[i-1][j], dp[i][j-1])`.
- Order: rows and columns increase, so the upper, left and diagonal cells are ready.
- Correctness: if the last characters are equal, an optimal subsequence can be changed to end with this pair. If they differ, at least one of them is not the last character of an optimal subsequence, so dropping it loses nothing and the maximum of the two subproblems is optimal.
- Reconstruction: start at `(n, m)`. On a match take the character and move diagonally. Otherwise move to a neighbour whose value still equals `dp[i][j]`, preferring `i-1` on ties. Every step preserves "remaining length = dp value", so the reversed characters form an optimal common subsequence. For `ab`/`ba` the result is `a`; `b` is equally valid.
- Time and memory: O((|a|+1)(|b|+1)) for the int table, O(|a|+|b|) for reconstruction and output.
- Boundary test: `axbycz`/`abc` → 3, `abc`. A substring DP would return 1.

### merge

- State: `dp[l][r]` is the minimum cost to merge original blocks `l..r` into one block, and `split[l][r]` is the `k` of its last merge.
- Base: `dp[i][i] = 0`.
- Transition: `dp[l][r] = min over l ≤ k < r of (dp[l][k] + dp[k+1][r]) + S(l, r)`, where `S` is computed from prefix sums. The last merge always joins `[l,k]` and `[k+1,r]` and costs `S(l,r)` whatever `k` is. The two sides are independent subproblems.
- Order: increasing interval length, so both shorter intervals are already solved.
- Correctness: in an optimal plan for `[l,r]` the merges inside each side must be optimal, otherwise replacing them would lower the total.
- Reconstruction: `addPlan(l,r)` emits the plan of `[l,k]`, then the plan of `[k+1,r]`, then `[l,k,r]`. A parent merge needs both child blocks to exist, so this postorder is chronological. The recursion depth is at most n ≤ 200. Ties keep the first minimal `k`. For n ≤ 1 the cost is 0 and the plan is empty.
- Time and memory: O(n³) time, O(n²) memory for two tables, O(n) plan. Costs are `long`; the maximum total is below 199 · 2·10¹¹.
- Boundary test: `[10,1,10,1]` → 44. Greedily merging the two smallest blocks would join the non-adjacent 1s (cost 36, invalid).

### tree

- State: `take[v]` is the best weight in v's subtree with v selected; `skip[v]` is the best with v not selected.
- Transition: `take[v] = w[v] + Σ skip[c]`, `skip[v] = Σ max(take[c], skip[c])` over children `c`. For a leaf, `take = w` and `skip = 0`.
- Order: rooted at 0. An iterative DFS with an explicit stack records each vertex after its parent; processing that order in reverse handles children before parents. With no recursion, a 100000-vertex chain is safe.
- Correctness: children's subtrees are disjoint and connected only through v, so the only constraint between them is v's state. The answer is `max(take[0], skip[0])`. It is at least 0 because the empty set is allowed, so all-negative inputs return 0. The rooting does not change the optimum, because the independent-set problem is defined on the undirected tree.
- Reconstruction: go top-down in DFS order. v is selected iff its parent is not selected and `take[v] > skip[v]`. A selected parent forces `skip`, matching the value used in `take[parent]`. Ties prefer `skip`; for `[5,5]` the result is `[1]`.
- Time and memory: O(n) for the adjacency lists, order, parent, stack and two `long` arrays. Sums are at most 10¹⁴ in absolute value.
- Boundary test: a star `[10,4,4,4]` → 12 from the three leaves. Picking the heaviest vertex first would give 10.

### naive

- Every start `i` from 0 to `n-m` is tested, since `n-m` is the last start where the window fits. Characters are compared until a mismatch, and the start is reported if all `m` match. Each start is independent, so overlaps are reported.
- With `m = 0` the loop runs over `i = 0..n` and every boundary matches, giving n+1 starts. With `m > n` the loop does not run.
- Time and memory: O(n·m + n + 1) time, O(1) auxiliary memory, O(z+1) output.
- Boundary test: `abc` with pattern `""` → `[0,1,2,3]`. This detects a missing final boundary.

### kmp

- `pi[i]` is the length of the longest proper prefix of `p[0..i]` that is also its suffix.
- Search invariant: after reading `t[i]`, `k` is the longest prefix of `p` that ends at `i`. On a mismatch, `k = pi[k-1]` tries the next shorter border, which is still a valid match ending. After a full match the start `i-m+1` is reported and `k = pi[m-1]`, which keeps overlapping matches. The fallback visits all candidate borders in decreasing order, so no occurrence is skipped.
- The empty pattern returns all n+1 boundaries before `pi` is built.
- Time and memory: O(n + m + z + 1). `k` increases at most once per character and every fallback decreases it. O(m) auxiliary memory for `pi`.
- Boundary test: `aaaa`/`aa` → `[0,1,2]`. Resetting `k = 0` after a match would lose start 1.

### rk

- Hash: `h(s) = Σ s[i]·31^(m-1-i) mod 101` over ASCII codes, with the highest power on the leftmost character, and `high = 31^(m-1) mod 101`.
- Rolling update: `H = ((H - t[i]·high) % 101 + 101) % 101`, then `H = (H·31 + t[i+m]) % 101`. The `+101` is needed because Java's `%` can return a negative value. Values stay small, so `long` cannot overflow.
- Equal strings always have equal hashes, so no match is missed. An equal hash only marks a candidate, and the characters are compared before reporting, which removes false positives.
- `m = 0` returns n+1 starts and `m > n` returns `[]`, both before any window is formed.
- Time and memory: O(n + m + c·m), where `c` is the number of equal-hash windows. In the worst case, such as `a^n`/`a^m`, every window is a candidate and the time is O(n·m), not linear. O(1) auxiliary memory plus output.
- Boundary test: `abdjab`/`ab` → `[0,4]`. The window `dj` at 2 also hashes to 75 and must be rejected.

### suffix: prefix doubling

- Invariant: after the round with step `k`, `rank[i]` orders suffixes by their first `2k` characters (equal rank iff those prefixes are equal). Initially the rank is the character code.
- A round sorts start indices by the pair `(rank[i], rank[i+k])`, using `-1` when `i+k ≥ n`. The `-1` makes a suffix that ends early (a prefix of the other) sort first, so no sentinel is added. New ranks 0, 1, … are assigned by comparing neighbouring pairs.
- The rounds stop when all ranks are distinct, at the latest when `2k ≥ n`. Suffix strings are never built.
- Time and memory: `Arrays.sort` with a comparator (TimSort) is O(n log n) per round over O(log n) rounds, so O(n log² n) total. Memory is O(n) for `idx`, `rank`, `tmp`, `sa` and `lcp`.

### suffix: Kasai LCP and longest repeat

- `rank` is the inverse of `sa`. Suffixes are processed in text order. For `rank[i] > 0`, suffix `i` is compared with its predecessor `j = sa[rank[i]-1]`, starting from the current `h`, and `lcp[rank[i]] = h`.
- Reuse: if suffix `i` shares `h` characters with its predecessor, suffix `i+1` shares at least `h-1` with its own predecessor, because dropping the first character keeps the order. So `h` drops by at most 1 per step, increases at most n times in total, and the pass is O(n). When `rank[i] = 0`, `lcp[0] = 0` and `h` is reset.
- A substring that occurs at two starts is a common prefix of two suffixes. The LCP of any two suffixes is the minimum of the adjacent LCPs between them in suffix-array order, so the largest pairwise LCP occurs at an adjacent pair: the answer is `max(lcp)`, and `repeated = text[sa[r] .. sa[r]+max)`. If the maximum is 0 the result is `""`.
- Boundary tests:
  - `abab` → `sa = [2,0,3,1]`: `ab` sorts before `abab` without a sentinel.
  - `a a` checks that a space (code 32) sorts before letters.
  - `mississippi` gives the overlapping repeat `issi` at starts 1 and 4.

### aho

- Trie: `go[state·95 + (c-32)]`, where 0 means "no child" (the root is never a child). Duplicate patterns end in the same state, and `term[p]` stores the state of each dictionary position.
- Failure links (BFS): a root child fails to the root. For a child `v` of `u` by character `c`, `fail[v] = go[fail[u]][c]`. Missing transitions are filled with `go[fail[u]][c]` (the root uses 0), which gives a complete automaton. `fail[u]` is shallower, so it is finished earlier in BFS order. `fail[v]` is the longest proper suffix of v's string that is also a trie state.
- Counting: for each text a cleared `visits` array is used. The text walks the automaton and adds `visits[state]++` per character. Then, in reverse BFS order, `visits[fail[v]] += visits[v]`. Every pattern that is a suffix of the current string lies on the failure chain, so suffix patterns (`he` inside `she`) are counted without listing individual matches. Each column is `visits[term[p]]`, so duplicates get equal counts.
- Reset: the automaton is built from local arrays for every request, and `visits` is cleared for every text. An empty dictionary gives one state and empty rows; an empty `texts` array gives `[]`.
- Time and memory: building is O(L + 95·S + p) time and O(95·S + p) memory, with the table allocated for at most L+1 states. Each text costs O(|text| + S + p), and the output is O(q·p). Counts are `long`.
- Boundary test: `["a","aa","aaa","a"]` on `aaaa` → `[4,3,2,4]`, which covers overlaps, suffix patterns and duplicates.

## Own tests


|Command|Input or fixture path|Expected result|Actual result / log|What it checks|
|---|---|---|---|---|
|lcs|cases.jsonl 1–2: `""`/`abc`, `""`/`""`|0, `""`|same, fixtures 1–2|empty inputs|
|lcs|cases.jsonl 3: `axbycz`/`abc`|3, `abc`|same, fixture 3|subsequence, not substring|
|lcs|cases.jsonl 4: `aaaa`/`aa`|2, `aa`|same, fixture 4|repeated characters|
|lcs|cases.jsonl 5: `ab`/`ba`|1, `a` (`b` also optimal)|same, fixture 5|tie in reconstruction|
|merge|cases.jsonl 6: `[5]`|0, `[]`|same, fixture 6|one block|
|merge|cases.jsonl 7: `[0,0,0]`|0, `[[1,1,2],[0,0,2]]`|same, fixture 7|zeros, n-1 merges|
|merge|cases.jsonl 8: `[1,1,1,1]`|8, `[[0,0,1],[2,2,3],[0,1,3]]`|same, fixture 8|several optimal plans, chronological order|
|merge|cases.jsonl 9: `[10,1,10,1]`|44|same, fixture 9|adjacent merge rule versus arbitrary greedy merging|
|tree|cases.jsonl 10–11: one vertex −5 / 7|0 `[]` / 7 `[0]`|same, fixtures 10–11|one vertex, negative weight|
|tree|cases.jsonl 12: path −1,−2,−3|0, `[]`|same, fixture 12|all negative weights → empty set|
|tree|cases.jsonl 13: edge with weights 5,5|5, `[1]` (`[0]` also optimal)|same, fixture 13|tie|
|tree|cases.jsonl 14: star 10/4,4,4|12, `[3,2,1]`|same, fixture 14|heaviest vertex is not optimal|
|tree|cases.jsonl 15: path 2,0,0,2, edges listed as `[child,parent]`|4, `[0,3]`|same, fixture 15|zero weights, undirected edges|
|naive/kmp/rk|cases.jsonl 16–30: `abc`/`""`, `""`/`a`, `A a, a!`/`a`, `aaaa`/`aa`, `abcab`/`abcab`|`[0,1,2,3]`, `[]`, `[2,5]`, `[0,1,2]`, `[0]`|same for all three, fixtures 16–30|empty pattern gives n+1 starts, empty text, case and punctuation, overlaps, whole text|
|rk|cases.jsonl 31–32: `abdjab`/`ab`, `djdj`/`ab`|`[0,4]`, `[]`|same, fixtures 31–32|hash 75 collision (`ab`/`dj`) rejected after verification|
|suffix|cases.jsonl 33: `aaaa`|sa `[3,2,1,0]`, lcp `[0,1,2,3]`, `aaa`|same, fixture 33|all equal characters, overlapping repeat|
|suffix|cases.jsonl 34: `abcd`|sa `[0,1,2,3]`, lcp zeros, `""`|same, fixture 34|all distinct, no repeat|
|suffix|cases.jsonl 35–36: `abab`, `a a`|`[2,0,3,1]`/`[0,2,0,1]`/`ab`; `[1,2,0]`/`[0,0,1]`/`a`|same, fixtures 35–36|no sentinel (prefix suffix first), space ordering|
|suffix|cases.jsonl 37: `mississippi`|sa `[10,7,4,1,0,9,8,6,3,5,2]`, lcp `[0,1,1,4,0,0,1,0,2,1,3]`, `issi`|same, fixture 37|standard example, overlapping repeat|
|suffix|cases.jsonl 38: `aabb`|sa `[0,1,3,2]`, lcp `[0,1,0,1]`, `a` (`b` also valid)|same, fixture 38|tied longest repeats|
|aho|cases.jsonl 39–40: no patterns; no texts|`[[],[]]`, `[]`|same, fixtures 39–40|empty dictionary, empty text list|
|aho|cases.jsonl 41: `a,aa,aaa,a` on `aaaa`,`""`,`baab`|`[[4,3,2,4],[0,0,0,0],[2,1,0,2]]`|same, fixture 41|overlaps, suffix patterns, duplicates, empty text|
|aho|cases.jsonl 42: `He,he, ,!` on `He said: he!`|`[[1,1,2,1]]`|same, fixture 42|case-sensitivity, spaces, punctuation|
|aho|cases.jsonl 43: `abcd,bc,c` on `abcabcd`|`[[1,2,2]]`|same, fixture 43|failure transitions after a partial match|
|aho|cases.jsonl 44–45: `abab` twice; line 41 repeated|`[[2],[2]]`; same as 41|same, fixtures 44–45|reset between texts and between consecutive requests|
|all|`StudentTests.oracles`, `Random(2026)`, 500 cases each|matches brute force|`PASS oracle ...` in results.txt|LCS vs subset enumeration; merge vs all adjacent orders with plan replay; tree vs all subsets with independence check; the three searches vs `startsWith` at every boundary; suffix vs sorted substrings and direct LCP; Aho vs counting every start|
|all|`StudentTests.scale`|known answers|`PASS scale ...` in results.txt|LCS 1000×1000; merge 200×10⁹ = 1544000000000; chain of 100000 → 5·10¹³ without recursion; KMP 100001 starts; RK with every window a candidate; suffix a^50000 and random 50000; dense Aho a^1..a^446; Aho with 10000 patterns × 100 texts vs a window-count oracle|

## Search comparison

`StudentTests.bench` uses n = 1000, 2000 and 3000, with m = n/10. For random cases it uses `Random(n)` over `abcd` and selects a pattern from the text. Repetitive cases use `a^n` and `a^m`. Every output is checked against an independent `startsWith` oracle. Timings cover the algorithm method call on a prepared request, excluding JSON parsing and output. Each algorithm is warmed up with 500 calls on every case; each run averages 200 calls measured with `System.nanoTime` in the same JVM. Values are microseconds per call. The table below was regenerated locally; the full run is in `tests/student/results.txt`.

| Case | n | m | Seed/generator | Algorithm | Run 1 | Run 2 | Run 3 | Median | Correct outputs checked |
|---|---:|---:|---|---|---:|---:|---:|---:|---|
| random | 1000 | 100 | Random(1000), abcd, window of text | naive | 2.6 us | 2.2 us | 2.1 us | 2.2 us | yes, z=1 |
| random | 1000 | 100 | Random(1000), abcd, window of text | kmp | 3.7 us | 3.0 us | 3.3 us | 3.3 us | yes, z=1 |
| random | 1000 | 100 | Random(1000), abcd, window of text | rk | 12.9 us | 12.7 us | 12.6 us | 12.7 us | yes, z=1 |
| random | 2000 | 200 | Random(2000), abcd, window of text | naive | 4.6 us | 4.8 us | 4.5 us | 4.6 us | yes, z=1 |
| random | 2000 | 200 | Random(2000), abcd, window of text | kmp | 6.2 us | 6.1 us | 6.1 us | 6.1 us | yes, z=1 |
| random | 2000 | 200 | Random(2000), abcd, window of text | rk | 26.5 us | 25.6 us | 27.4 us | 26.5 us | yes, z=1 |
| random | 3000 | 300 | Random(3000), abcd, window of text | naive | 6.8 us | 6.9 us | 6.2 us | 6.8 us | yes, z=1 |
| random | 3000 | 300 | Random(3000), abcd, window of text | kmp | 9.3 us | 9.2 us | 9.2 us | 9.2 us | yes, z=1 |
| random | 3000 | 300 | Random(3000), abcd, window of text | rk | 39.0 us | 37.3 us | 38.2 us | 38.2 us | yes, z=1 |
| repetitive | 1000 | 100 | a^n, a^m | naive | 132.0 us | 131.0 us | 130.8 us | 131.0 us | yes, z=901 |
| repetitive | 1000 | 100 | a^n, a^m | kmp | 10.9 us | 6.4 us | 6.5 us | 6.5 us | yes, z=901 |
| repetitive | 1000 | 100 | a^n, a^m | rk | 215.9 us | 215.5 us | 214.4 us | 215.5 us | yes, z=901 |
| repetitive | 2000 | 200 | a^n, a^m | naive | 494.5 us | 490.8 us | 506.8 us | 494.5 us | yes, z=1801 |
| repetitive | 2000 | 200 | a^n, a^m | kmp | 12.7 us | 12.4 us | 12.4 us | 12.4 us | yes, z=1801 |
| repetitive | 2000 | 200 | a^n, a^m | rk | 817.7 us | 821.1 us | 806.8 us | 817.7 us | yes, z=1801 |
| repetitive | 3000 | 300 | a^n, a^m | naive | 1088.2 us | 1082.2 us | 1090.0 us | 1088.2 us | yes, z=2701 |
| repetitive | 3000 | 300 | a^n, a^m | kmp | 18.4 us | 18.7 us | 18.9 us | 18.7 us | yes, z=2701 |
| repetitive | 3000 | 300 | a^n, a^m | rk | 1779.2 us | 1764.5 us | 1762.4 us | 1764.5 us | yes, z=2701 |

Observations:

- Random inputs usually mismatch early, so naive can be fastest at these sizes. KMP computes the prefix table, and RK pays for rolling hash operations and verifies occasional collision candidates.
- On repetitive inputs every window matches. Naive and exact RK verify all m characters at each start and exhibit quadratic work; KMP remains linear in text plus pattern length and reported matches.
- Timings depend on this machine and other running processes. They illustrate the measured cases and do not prove asymptotic bounds.


## Sources

- Course materials: the official Assignment 4 specification and the week 8–10 course lectures
- Verification performed in this session: `python tools/run.py --setup`, `python tools/run.py --check`, and the Java `StudentTests` suite all passed locally. The suite includes hand-checked fixtures, 500 randomized brute-force oracle cases per algorithm family, maximum-size inputs and the search benchmark. Full output is in `tests/student/results.txt`. 
