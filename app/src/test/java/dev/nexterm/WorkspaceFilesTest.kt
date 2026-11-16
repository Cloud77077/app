package dev.nexterm
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
class WorkspaceFilesTest {
 @Test fun savePreservesUnicodeAndReplacesOldContent() {
  val root=Files.createTempDirectory("nexterm-test").toFile()
  try { val f=WorkspaceFiles.create(root,"code.kt",false);f.writeText("old")
   WorkspaceFiles.save(f,"λ hello 🌍\n");assertEquals("λ hello 🌍\n",f.readText());assertEquals(listOf("code.kt"),root.list()!!.toList())
  } finally {root.deleteRecursively()}
 }
 @Test fun rejectsTraversalAndExistingFiles() {
  val root=Files.createTempDirectory("nexterm-test").toFile()
  try {
   listOf("../escape","/tmp/escape","..",".","a/b","a\\b","").forEach {name -> assertThrows(IllegalArgumentException::class.java){WorkspaceFiles.create(root,name,false)} }
   WorkspaceFiles.create(root,"existing",false).writeText("keep")
   assertThrows(IllegalStateException::class.java){WorkspaceFiles.create(root,"existing",false)}
   assertEquals("keep",root.resolve("existing").readText())
  } finally {root.deleteRecursively()}
 }
}
