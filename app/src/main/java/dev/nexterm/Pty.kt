package dev.nexterm
object Pty {
 init { System.loadLibrary("nexterm") }
 external fun open(directory: String): IntArray?
 external fun read(fd: Int): ByteArray?
 external fun write(fd: Int, data: ByteArray): Boolean
 external fun resize(fd: Int, rows: Int, cols: Int)
 external fun close(fd: Int, pid: Int)
}
