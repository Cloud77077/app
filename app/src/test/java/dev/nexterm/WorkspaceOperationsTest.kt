package dev.nexterm

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class WorkspaceOperationsTest {
 @Test fun moveSearchAndRecursiveDelete() {
  val root=Files.createTempDirectory("workspace").toFile()
  try {
   val f=WorkspaceFiles.create(root,"a.txt",false).apply{writeText("hello\nworld λ")}
   WorkspaceFiles.create(root,"src",true)
   val moved=WorkspaceFiles.move(root,f,"src/b.txt")
   assertFalse(f.exists());assertTrue(moved.exists())
   val matches=WorkspaceFiles.search(root,"WORLD")
   assertEquals(1,matches.size);assertEquals(2,matches.single().line)
   WorkspaceFiles.delete(root,root.resolve("src"));assertEquals(0,root.list()!!.size)
  } finally { root.deleteRecursively() }
 }
 @Test fun preventsEscapeOverwriteAndRootDeletion() {
  val root=Files.createTempDirectory("workspace").toFile()
  val outside=Files.createTempDirectory("outside").toFile()
  try {
   val f=WorkspaceFiles.create(root,"a",false)
   WorkspaceFiles.create(root,"b",false)
   assertThrows(IllegalArgumentException::class.java){WorkspaceFiles.move(root,f,"b")}
   assertThrows(IllegalArgumentException::class.java){WorkspaceFiles.move(root,f,"../escape")}
   assertThrows(IllegalArgumentException::class.java){WorkspaceFiles.delete(root,root)}
   Files.createSymbolicLink(root.resolve("link").toPath(),outside.toPath())
   assertThrows(IllegalArgumentException::class.java){WorkspaceFiles.move(root,f,"link/escape")}
   outside.resolve("keep").writeText("safe")
   assertThrows(IllegalArgumentException::class.java){WorkspaceFiles.delete(root,root.resolve("link"))}
   assertTrue(outside.resolve("keep").exists());assertTrue(WorkspaceFiles.search(root,"safe").isEmpty())
  } finally { root.resolve("link").delete();root.deleteRecursively();outside.deleteRecursively() }
 }
}
