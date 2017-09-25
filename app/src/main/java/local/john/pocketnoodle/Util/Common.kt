package local.john.pocketnoodle.Util

import android.content.Context
import android.widget.Toast
import org.json.JSONArray

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

    internal fun remove(name: String): Boolean {
        for(pos in snakes.indices) {
            val snake = snakes[pos]

            if(snake.name == name) {
                snakes.removeAt(pos)
                return true
            }
        }
        return false
    }

    internal fun add(name: String): Boolean {
        if(snakes.any { it.name == name })
            return false

        return snakes.add(Snake(name, mutableListOf(), mutableListOf()))
    }

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

internal fun parseJSON(list: JSONArray): MutableList<Snake> {
    var i = 0
    val temp = mutableListOf<Snake>()

    while(i < list.length()) {
        val cur = list.getJSONObject(i++)
        val name = cur.getString("name")
        val feeds = cur.getJSONArray("feeds")
        val sheds = cur.getJSONArray("sheds")

        val tempFeeds = mutableListOf<String>()
        val tempSheds = mutableListOf<String>()

        var pos = 0
        while(pos < feeds.length())
            tempFeeds.add(feeds[pos++].toString())

        pos = 0
        while(pos < sheds.length())
            tempSheds.add(sheds[pos++].toString())

        temp.add(Snake(name, tempFeeds, tempSheds))
    }

    return temp
}

fun Context.toast(message: CharSequence) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()