package local.john.pocketnoodle.Util

internal data class Snakes(private val snakes: MutableList<Snake>) {
    override fun toString() = snakes.toString()

    internal fun updateAll(newList: MutableList<Snake>) {
        snakes.clear()
        snakes.addAll(newList)
    }
    internal fun update(newSnake: Snake) {
        snakes.removeAt(snakes.indices.map { it to snakes[it] }
                                      .filter { (_, snake) -> snake.name == newSnake.name }[0]
                                      .first)
        snakes.add(newSnake)
    }
    internal fun get(pos: Int) = if(pos < size) snakes[pos]
                                 else null
    internal fun get(name: String) = snakes.firstOrNull { it.name == name }

    internal fun getNames() = snakes.map { it.name }.toTypedArray()

    private val size: Int
        get() = snakes.size
}
internal data class Snake(internal val name: String,
                          internal val feedDates: MutableList<String>,
                          internal val shedDates: MutableList<String>) {
    override fun toString(): String {
        val feeds = when {
            feedDates.size > 1 -> try { feedDates.map { """"$it"""" }.reduce { a,b -> "$a,$b" } } catch (e: UnsupportedOperationException) { "" }
            feedDates.size == 1 -> """"${feedDates[0]}""""
            else -> ""
        }
        val sheds = when {
            shedDates.size > 1 -> try { shedDates.map { """"$it"""" }.reduce { a, b -> "$a,$b" } } catch (e: UnsupportedOperationException) { "" }
            shedDates.size == 1 -> """"${shedDates[0]}""""
            else -> ""
        }

        return """{"name":"$name","feeds":[$feeds],"sheds":[$sheds]}"""
    }
}