package dev.nexterm

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.SimpleFileVisitor
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

object WorkspaceFiles {
 private fun inside(root:File,file:File):File = file.canonicalFile.also {
  require(it.toPath().startsWith(root.canonicalFile.toPath())) { "Path is outside the workspace" }
 }
 fun move(root:File,source:File,destination:String):File {
  val from=inside(root,source)
  require(from != root.canonicalFile) { "Cannot move workspace root" }
  require(destination.isNotBlank() && !File(destination).isAbsolute) { "Use a workspace-relative path" }
  val to=inside(root,File(root,destination))
  require(!to.exists()) { "Destination already exists" }
  require(to.parentFile?.isDirectory == true) { "Destination folder does not exist" }
  Files.move(from.toPath(),to.toPath())
  return to
 }
 fun delete(root:File,file:File) {
  val target=inside(root,file)
  require(target != root.canonicalFile) { "Cannot delete workspace root" }
  Files.walkFileTree(target.toPath(),object:SimpleFileVisitor<Path>() {
   override fun visitFile(path:Path,attrs:BasicFileAttributes):FileVisitResult { Files.delete(path);return FileVisitResult.CONTINUE }
   override fun postVisitDirectory(path:Path,error:java.io.IOException?):FileVisitResult {
    if(error!=null) throw error
    Files.delete(path);return FileVisitResult.CONTINUE
   }
  })
 }
 data class Match(val file:File,val line:Int,val preview:String)
 fun search(root:File,query:String):List<Match> {
  if(query.isBlank()) return emptyList()
  val results=mutableListOf<Match>()
  Files.walkFileTree(root.toPath(),object:SimpleFileVisitor<Path>() {
   override fun visitFile(path:Path,attrs:BasicFileAttributes):FileVisitResult {
    if(attrs.isRegularFile && attrs.size() <= 1_000_000) {
     runCatching { path.toFile().bufferedReader().useLines { lines ->
      lines.forEachIndexed { index,line -> if(results.size < 200 && line.contains(query,true)) results.add(Match(path.toFile(),index+1,line.take(240))) }
     } }
    }
    return if(results.size >= 200) FileVisitResult.TERMINATE else FileVisitResult.CONTINUE
   }
  })
  return results
 }
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
