package dev.nexterm

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object WorkspaceFiles {
 fun create(directory: File, name: String, folder: Boolean): File {
  require(name.isNotBlank() && name != "." && name != ".." && !name.contains('/') && !name.contains('\\')) { "Enter a single valid name" }
  return File(directory, name).also {
   check(if (folder) it.mkdir() else it.createNewFile()) { "Already exists or cannot create" }
  }
 }
 fun save(file: File, text: String) {
  val temp=File.createTempFile(".nexterm-", ".tmp", file.parentFile)
  try {
   temp.outputStream().use { out -> out.write(text.toByteArray(Charsets.UTF_8));out.fd.sync() }
   Files.move(temp.toPath(),file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING)
  } finally { temp.delete() }
 }
}
