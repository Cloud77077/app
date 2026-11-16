package dev.nexterm

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class EditorDocumentTest {
 @Test fun undoRedoDirtyAndSave() {
  val root=Files.createTempDirectory("editor").toFile()
  try {
   val file=root.resolve("code.txt").apply{writeText("old")}
   val d=EditorDocument(file);assertFalse(d.dirty)
   d.edit("λ 🌍");assertTrue(d.dirty);d.undo();assertEquals("old",d.content)
   d.redo();assertEquals("λ 🌍",d.content);d.save();assertFalse(d.dirty)
   assertEquals("λ 🌍",file.readText());d.undo();d.edit("new")
   assertFalse(d.canRedo);assertTrue(d.dirty)
  } finally { root.deleteRecursively() }
 }
}
