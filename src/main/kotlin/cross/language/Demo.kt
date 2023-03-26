package cross.language

import ch.uzh.ifi.seal.changedistiller.JavaChangeDistillerModule
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange
import com.google.inject.Guice
import cross.language.algorithm.HungarianAlgorithm
import cross.language.parser.CSharpParser
import cross.language.parser.JavaParser
import cross.language.algorithm.editDistance
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.Integer.max

fun run() {
    val javaFiles: List<FileInfo> = Json.decodeFromString(File("data/javaFiles.txt").readText())
    val csharpFiles: List<FileInfo> = Json.decodeFromString(File("data/csharpFiles.txt").readText())
    println(javaFiles[111].path)

    val bugs: List<FileInfo> = Json.decodeFromString(File("data/bugs.txt").readText())

//        var count = 0
    val debug = true

    for (i in bugs.indices step 2) {
        if (debug) {
            if (i > 100) break
            println("-------------------------------------------------------------------------------------")
            println("${i / 2}: ${bugs[i].fullPath}")
        }

        val from = bugs[i]
        val to = bugs[i + 1]
        val other = csharpFiles.find { it.formattedFileName == to.formattedFileName }   // TODO find best match

        // hungarian algorithm
        if (other != null) {

            val methodsJava = JavaParser(File(from.fullPath).inputStream()).methods.map { it.name }
            val methodsCSharp = CSharpParser(File(other.fullPath).inputStream()).methods.map { it.name }

            if (debug) {
//                    for (name in methodsJava) println(name)
//                    println("###########################################################################################")
//                    for (name in methodsCSharp) println(name)

                val size = max(methodsJava.size, methodsCSharp.size)
                val distanceList = List<Int>(size * size) {
                    val x = it / size
                    val y = it % size
                    editDistance(methodsJava.getOrElse(x) { "" }, methodsCSharp.getOrElse(y) { "" })
                }

//                    println(distanceList)

                val h = HungarianAlgorithm(distanceList)

//                    println(h.input)
//                    println(h.final_assignment)
//                    println(h.final_cost)

                for (i in 0 until size) {
                    println("${methodsJava.getOrElse(i) { "" }}\t\t\t${methodsCSharp.getOrElse(h.finalAssignment[i]) { "" }}")
                }
            }

        }


    }
}

fun distillerRun(left: File, right: File): List<SourceCodeChange> {
    val injector = Guice.createInjector(JavaChangeDistillerModule())
    val distiller = injector.getInstance(FileDistiller::class.java)
    try {
        distiller.extractClassifiedSourceCodeChanges(left, right)
    } catch (e: Exception) {
        System.err.println("Warning: error while change distilling. " + e.message)
    }
    return distiller.sourceCodeChanges ?: emptyList()
}

fun findOther(fileInfo: FileInfo, files: List<FileInfo>): FileInfo? =
    files.find { it.formattedFileName == fileInfo.formattedFileName }

fun findOther(formattedFileName: String, files: List<FileInfo>): FileInfo? =
    files.find { it.formattedFileName == formattedFileName }

@Serializable
class FileInfo(
    val path: String,
    val root: String,
    val fileName: String,
    val formattedFileName: String,
    val tags: MutableList<String> = mutableListOf(),
    val mathods: MutableList<String> = mutableListOf()
) {
    override fun toString() = "${this.root}${this.path}"
    val fullPath = "${this.root}${this.path}"
}

