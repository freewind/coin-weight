package example

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import java.io.File

object DataFiles {
    private val root = File("data")
    val coinsFile = File(root, "coins.json")
    fun historyDataFile(coin: Coin): File = File(root, "history/${coin.website_slug}.json")
}

fun saveCoins(coins: List<Coin>) {
    val json = Klaxon().toJsonString(coins)
    DataFiles.coinsFile.writeText(json)
}

fun loadCoins(): List<Coin>? {
    return if (DataFiles.coinsFile.exists()) {
        (Parser().parse(DataFiles.coinsFile.absolutePath) as JsonArray<JsonObject>).let(::buildCoins)
    } else {
        null
    }
}

fun saveHistoryData(coin: Coin, data: List<HistoryData>) {
    val json = Klaxon().toJsonString(data)
    DataFiles.historyDataFile(coin).writeText(json)
}

fun loadHistoryData(coin: Coin): List<HistoryData>? {
    val file = DataFiles.historyDataFile(coin)
    return if (file.exists()) {
        val array = (Parser().parse(file.absolutePath) as JsonArray<JsonObject>)
        return array.map {
            HistoryData(
                    date = it.string("date")!!,
                    openPrice = it.string("openPrice")!!,
                    highPrice = it.string("highPrice")!!,
                    lowPrice = it.string("lowPrice")!!,
                    closePrice = it.string("closePrice")!!,
                    volume = it.string("volume")!!,
                    marketCap = it.string("marketCap")!!
            )
        }
    } else {
        null
    }
}


