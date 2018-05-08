package example

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import org.apache.commons.io.IOUtils
import org.jsoup.Jsoup
import java.io.FileNotFoundException
import java.io.StringReader
import java.net.URI
import java.net.URL
import java.text.SimpleDateFormat

data class HistoryData(val date: String, val openPrice: String, val highPrice: String, val lowPrice: String, val closePrice: String, val volume: String, val marketCap: String)
data class Coin(val name: String, val symbol: String, val website_slug: String)

object Urls {
    const val listings = "https://api.coinmarketcap.com/v2/listings/"
    fun history(symbol: String) = "https://coinmarketcap.com/currencies/$symbol/historical-data/?start=20000101&end=21000000"
}

private fun fetchHistoryDataUntilSuccessful(coin: Coin): List<HistoryData>? {
    return try {
        fetchCoinHistoryData(coin)
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
        null
    } catch (e: Exception) {
        e.printStackTrace()
        fetchHistoryDataUntilSuccessful(coin)
    }
}

fun fetchAllCoins(): List<Coin> {
    val json = URL(Urls.listings).readText()
    return (Parser().parse(StringReader(json)) as JsonObject)
            .array<JsonObject>("data")!!
            .let(::buildCoins)
}

fun buildCoins(jsonArray: JsonArray<JsonObject>): List<Coin> {
    return jsonArray.map { Coin(it.string("name")!!, it.string("symbol")!!, it.string("website_slug")!!) }
}

fun fetchCoinHistoryData(coin: Coin): List<HistoryData> {
    val url = Urls.history(coin.website_slug)
    println(url)
    val html = IOUtils.toString(URI(url), "utf-8")
    val doc = Jsoup.parse(html)
    val rows = doc.selectFirst("#historical-data table tbody").children()
    return rows.map {
        val tds = it.selectFirst("tr").children()
        val date = tds[0].text().let(::toDate)
        val open = tds[1].text().let(::toDouble)
        val high = tds[2].text().let(::toDouble)
        val low = tds[3].text().let(::toDouble)
        val close = tds[4].text().let(::toDouble)
        val volume = tds[5].text().let(::toDouble)
        val marketCap = tds[6].text().let(::toDouble)
        HistoryData(date, open, high, low, close, volume, marketCap)
    }
}

private fun toDate(str: String): String {
    return SimpleDateFormat("MMM dd, yyyy").parse(str).let {
        SimpleDateFormat("yyyy-MM-dd").format(it)
    }
}

private fun toDouble(str: String): String {
    return str.replace(",", "").let {
        when (it) {
            "" -> "0"
            else -> it
        }
    }
}

fun main(args: Array<String>) {
    val coinsFile = DataFiles.coinsFile
    val coins = if (coinsFile.exists()) {
        loadCoins(coinsFile)
    } else {
        fetchAllCoins().apply(::saveCoins)
    }

    coins.forEach { coin ->
        val historyFile = DataFiles.historyDataFile(coin)
        if (!historyFile.exists()) {
            val data = fetchHistoryDataUntilSuccessful(coin)
            if (data != null) {
                saveHistoryData(coin, data)
            }
        }
    }
}
