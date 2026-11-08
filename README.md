# Assignment 4 - Dynamic Programming and String Processing

Java 17: LCS, optimal adjacent block merging, maximum-weight independent set on a tree, naive/KMP/Rabin–Karp string search, suffix array with Kasai LCP, and Aho–Corasick.

The program reads UTF-8 JSON Lines from standard input. From the project folder, run the public checks and student tests with Python 3.12+ and JDK 17+:

```powershell
py -3 tools/run.py --check
javac --release 17 -encoding UTF-8 -d Java/build Java/source/*.java tests/student/StudentTests.java
java -Xmx384m -cp Java/build StudentTests
```
