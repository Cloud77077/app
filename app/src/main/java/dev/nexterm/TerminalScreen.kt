package dev.nexterm

/** Bounded VT-style screen. Unsupported control sequences are consumed, never printed. */
class TerminalScreen(var rows: Int = 24, var columns: Int = 80, private val historyLimit: Int = 10_000) {
 data class Cell(val text: String = " ", val foreground: Int = 7, val background: Int = 0, val bold: Boolean = false)
 private fun blank() = MutableList(columns) { Cell() }
 private var screen = MutableList(rows) { blank() }
 private val history = ArrayDeque<List<Cell>>()
 private var primary: MutableList<MutableList<Cell>>? = null
 private var savedRow = 0
 private var savedColumn = 0
 private var primaryRow = 0
 private var primaryColumn = 0
 var row = 0; private set
 var column = 0; private set
 var cursorVisible = true; private set
 private var foreground = 7
 private var background = 0
 private var bold = false
 private var top = 0
 private var bottom = rows - 1
 private var wrapPending = false
 private var state = 0
 private val sequence = StringBuilder()
 private var highSurrogate: Char? = null
 var reply: (String) -> Unit = {}
 val alternate: Boolean get() = primary != null
 fun lines(): List<List<Cell>> = (if (alternate) emptyList() else history.toList()) + screen.map { it.toList() }
 fun text(): String = lines().joinToString("\n") { line -> line.joinToString("") { it.text }.trimEnd() }.trimEnd()
 fun clearHistory() { history.clear() }
 fun resize(newRows: Int, newColumns: Int) {
  val r = newRows.coerceIn(2, 200); val c = newColumns.coerceIn(2, 300)
  if (r == rows && c == columns) return
  val shift=(row-r+1).coerceAtLeast(0)
  if(!alternate) { screen.take(shift).forEach { history.addLast(it.toList()) };while(history.size>historyLimit)history.removeFirst() }
  fun fit(source: MutableList<MutableList<Cell>>,offset:Int): MutableList<MutableList<Cell>> =
   MutableList(r) { y -> MutableList(c) { x -> source.getOrNull(y+offset)?.getOrNull(x) ?: Cell() } }
  screen = fit(screen,shift)
  val primaryShift=(primaryRow-r+1).coerceAtLeast(0)
  primary = primary?.let { fit(it,primaryShift) };primaryRow-=primaryShift
  rows = r; columns = c; row = (row-shift).coerceAtMost(r-1); column = column.coerceAtMost(c-1)
  top = 0; bottom = r-1; wrapPending = false
 }
 fun feed(text: String) {
  for (ch in text) {
   when (state) {
    1 -> { state = 0; when(ch) {
     '[' -> { state = 2; sequence.clear() }
     ']' -> state = 3
     'P', '^', '_' -> state = 3
     '7' -> { savedRow = row; savedColumn = column }
     '8' -> { row = savedRow.coerceIn(0,rows-1); column = savedColumn.coerceIn(0,columns-1); wrapPending = false }
     'D' -> newline()
     'M' -> if (row == top) { screen.removeAt(bottom); screen.add(top,blank()) } else row = (row-1).coerceAtLeast(0)
     'E' -> { column = 0; newline() }
     'c' -> reset()
     '(', ')' -> state = 5
    } }
    2 -> if (ch in '@'..'~') { control(ch,sequence.toString()); state = 0 } else if(sequence.length < 256) sequence.append(ch) else state = 0
    3 -> when(ch) { '\u0007' -> state = 0; '\u001b' -> state = 4 }
    4 -> { state = if(ch == '\\') 0 else 3 }
    5 -> state = 0
    else -> when(ch) {
     '\u001b' -> state = 1
     '\r' -> { column = 0; wrapPending = false }
     '\n', '\u000b', '\u000c' -> newline()
     '\b' -> { column = (column-1).coerceAtLeast(0); wrapPending = false }
     '\t' -> { column = ((column/8+1)*8).coerceAtMost(columns-1); wrapPending = false }
     else -> if(ch >= ' ' && ch != '\u007f') {
      if(ch.isHighSurrogate()) highSurrogate = ch
      else { val value = highSurrogate?.let { "$it$ch" } ?: ch.toString(); highSurrogate = null; put(value) }
     }
    }
   }
  }
 }
 private fun put(value: String) {
  if (wrapPending) { column = 0; newline() }
  screen[row][column] = Cell(value,foreground,background,bold)
  if(column == columns-1) wrapPending = true else column++
 }
 private fun newline() {
  wrapPending = false
  if(row == bottom) {
   val removed = screen.removeAt(top)
   if(top == 0 && bottom == rows-1 && !alternate) { history.addLast(removed.toList()); while(history.size > historyLimit) history.removeFirst() }
   screen.add(bottom,blank())
  } else row = (row+1).coerceAtMost(rows-1)
 }
 private fun reset() {
  primary = null; screen = MutableList(rows) { blank() }; history.clear()
  row = 0; column = 0; foreground = 7; background = 0; bold = false
  top = 0; bottom = rows-1; wrapPending = false; cursorVisible = true
 }
 private fun control(command: Char, raw: String) {
  val isPrivate = raw.startsWith('?')
  val values = raw.removePrefix("?").split(';').map { it.toIntOrNull() ?: 0 }
  fun value(index: Int = 0, default: Int = 1) = values.getOrElse(index) { 0 }.let { if(it == 0) default else it }
  val n = value().coerceAtMost(10000)
  when(command) {
   'A' -> row = (row-n).coerceAtLeast(top)
   'B' -> row = (row+n).coerceAtMost(bottom)
   'C' -> column = (column+n).coerceAtMost(columns-1)
   'D' -> column = (column-n).coerceAtLeast(0)
   'E' -> { row = (row+n).coerceAtMost(rows-1); column = 0 }
   'F' -> { row = (row-n).coerceAtLeast(0); column = 0 }
   'G', '`' -> column = (n-1).coerceIn(0,columns-1)
   'd' -> row = (n-1).coerceIn(0,rows-1)
   'H', 'f' -> { row = (value()-1).coerceIn(0,rows-1); column = (value(1)-1).coerceIn(0,columns-1) }
   'J' -> when(values[0]) {
    0 -> { erase(row,column,columns); for(y in row+1 until rows) erase(y,0,columns) }
    1 -> { for(y in 0 until row) erase(y,0,columns); erase(row,0,column+1) }
    2 -> screen = MutableList(rows) { blank() }
    3 -> history.clear()
   }
   'K' -> when(values[0]) { 0 -> erase(row,column,columns); 1 -> erase(row,0,column+1); 2 -> erase(row,0,columns) }
   'X' -> erase(row,column,(column+n).coerceAtMost(columns))
   'P' -> repeat(n.coerceAtMost(columns-column)) { screen[row].removeAt(column); screen[row].add(Cell()) }
   '@' -> repeat(n.coerceAtMost(columns-column)) { screen[row].add(column,Cell()); screen[row].removeAt(columns) }
   'L' -> if(row in top..bottom) repeat(n.coerceAtMost(bottom-row+1)) { screen.removeAt(bottom); screen.add(row,blank()) }
   'M' -> if(row in top..bottom) repeat(n.coerceAtMost(bottom-row+1)) { screen.removeAt(row); screen.add(bottom,blank()) }
   'S' -> repeat(n.coerceAtMost(rows)) { screen.removeAt(top); screen.add(bottom,blank()) }
   'T' -> repeat(n.coerceAtMost(rows)) { screen.removeAt(bottom); screen.add(top,blank()) }
   'r' -> { val t = value()-1; val b = value(1,rows)-1; if(t in 0 until b && b < rows) { top=t; bottom=b; row=0; column=0 } }
   's' -> { savedRow=row; savedColumn=column }
   'u' -> { row=savedRow.coerceIn(0,rows-1); column=savedColumn.coerceIn(0,columns-1) }
   'n' -> if(values[0] == 6) reply("\u001b[${row+1};${column+1}R") else if(values[0] == 5) reply("\u001b[0n")
   'c' -> reply("\u001b[?1;0c")
   'h', 'l' -> if(isPrivate) values.forEach { mode -> when(mode) {
    25 -> cursorVisible = command == 'h'
    1049 -> if(command == 'h' && primary == null) {
     primary=screen; primaryRow=row; primaryColumn=column; screen=MutableList(rows){blank()}; row=0; column=0
    } else if(command == 'l' && primary != null) { screen=primary!!; primary=null; row=primaryRow.coerceAtMost(rows-1); column=primaryColumn.coerceAtMost(columns-1) }
   } }
   'm' -> {
    var i=0
    while(i < values.size) { val v=values[i]; when(v) {
     0 -> { foreground=7; background=0; bold=false }
     1 -> bold=true
     22 -> bold=false
     39 -> foreground=7
     49 -> background=0
     in 30..37 -> foreground=v-30
     in 40..47 -> background=v-40
     in 90..97 -> foreground=v-90+8
     in 100..107 -> background=v-100+8
     38,48 -> if(values.getOrNull(i+1)==5 && values.getOrNull(i+2)!=null) {
      if(v==38) foreground=values[i+2].coerceIn(0,255) else background=values[i+2].coerceIn(0,255); i+=2
     } else if(values.getOrNull(i+1)==2 && i+4 < values.size) {
      val rgb=0x1000000 or (values[i+2].coerceIn(0,255) shl 16) or (values[i+3].coerceIn(0,255) shl 8) or values[i+4].coerceIn(0,255)
      if(v==38) foreground=rgb else background=rgb; i+=4
     }
    }; i++ }
   }
  }
  if(command != 'm') wrapPending = false
 }
 private fun erase(y:Int,start:Int,end:Int) { for(x in start until end) screen[y][x]=Cell(foreground=foreground,background=background) }
}
