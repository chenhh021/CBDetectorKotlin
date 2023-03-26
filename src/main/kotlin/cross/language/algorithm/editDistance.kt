package cross.language.algorithm

import java.util.Collections.min

//
//fun min(x: Int, y: Int, z: Int): Int {
//    if (x <= y && x <= z) return x
//    return if (y <= x && y <= z) y else z
//}

//fun editDistance(str1: String, str2: String, M: Int = -1, N: Int = -1): Int {
//    var m = M
//    var n = N
//    if (m == -1) m = str1.length
//    if (n == -1) n = str2.length
//    // If first string is empty, the only option is to
//    // insert all characters of second string into first
//    if (m == 0) return n
//
//    // If second string is empty, the only option is to
//    // remove all characters of first string
//    if (n == 0) return m
//
//    // If last characters of two strings are same,
//    // nothing much to do. Ignore last characters and
//    // get count for remaining strings.
//    return if (str1[m - 1] == str2[n - 1])
//        editDistance(str1, str2, m - 1, n - 1)
//    else (1 + min(
//        listOf(
//            editDistance(str1, str2, m, n - 1),  // Insert
//            editDistance(str1, str2, m - 1, n),  // Remove
//            editDistance(str1, str2, m - 1, n - 1) // Replace
//        )
//    ))
//}

fun editDistance(s1: String, s2: String): Int {
    val dis = MutableList<MutableList<Int>>(s1.length + 1) {
        MutableList<Int>(s2.length + 1) { 0 }
    }
    for (i in 0..s1.length)
        dis[i][0] = i
    for (j in 0..s2.length)
        dis[0][j] = j

    for (j in 1..s2.length) {
        for (i in 1..s1.length) {
            dis[i][j] = listOf(
                dis[i - 1][j] + 1,
                dis[i][j - 1] + 1,
                dis[i - 1][j - 1] + (s1[i - 1] != s2[j - 1]).toInt(),
            ).min();
        }
    }
    return dis[s1.length][s2.length];
}

fun Boolean.toInt() = if (this) 1 else 0