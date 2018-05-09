package example

import java.time.LocalDate

fun main(args: Array<String>) {
    val coins = loadCoins()!!
    val coinsData = coins.map { coin -> coin to loadHistoryData(coin) }.toMap().removeNullValues()
    val maxDate = coinsData.values.map { it.map { it.date }.max()!! }.max()!!

    val maxDayWeight = weightOfDate(maxDate, coinsData)
    val lastDayWeight = weightOfDate(getTheDayBefore(maxDate, 1), coinsData)
    val lastWeekWeight = weightOfDate(getTheDayBefore(maxDate, 7), coinsData)
    val last2WeeksWeight = weightOfDate(getTheDayBefore(maxDate, 14), coinsData)
    val lastMonthWeight = weightOfDate(getTheDayBefore(maxDate, 29), coinsData)

    val totalCoin = Coin("ALL", "ALL", "ALL")
    val maxTotalMarketCap = listOf(maxDayWeight, lastDayWeight, lastWeekWeight, last2WeeksWeight, lastMonthWeight).map { it.totalMarketCap }.max()!!
    val totalRow = Row(totalCoin,
            weightNow = maxDayWeight.totalMarketCap / maxTotalMarketCap,
            weightLastDay = lastDayWeight.totalMarketCap / maxTotalMarketCap,
            weightLastWeek = lastWeekWeight.totalMarketCap / maxTotalMarketCap,
            weightLast2Weeks = last2WeeksWeight.totalMarketCap / maxTotalMarketCap,
            weightLastMonth = lastMonthWeight.totalMarketCap / maxTotalMarketCap
    )

    val totalCoins = listOf(maxDayWeight, lastDayWeight, lastWeekWeight, last2WeeksWeight, lastMonthWeight).flatMap { it.weights.keys }.toSet()

    val x = totalCoins.map { coin ->
        val weightNow = maxDayWeight.weights[coin] ?: 0.0
        Row(
                coin = coin,
                weightNow = weightNow,
                weightLastDay = lastDayWeight.weights[coin] ?: 0.0,
                weightLastWeek = lastWeekWeight.weights[coin] ?: 0.0,
                weightLast2Weeks = last2WeeksWeight.weights[coin] ?: 0.0,
                weightLastMonth = lastMonthWeight.weights[coin] ?: 0.0
        )
    }


    printTable(maxDate, "当前市值前50名", listOf(totalRow) + x.sortedByDescending { it.weightNow }.take(50))
    printTable(maxDate, "1天内市值变化前50名", x.sortedByDescending { it.changeFromLastDay }.take(50))
    printTable(maxDate, "1周内市值变化前50名", x.sortedByDescending { it.changeFromLastWeek }.take(50))
    printTable(maxDate, "2周内市值变化前50名", x.sortedByDescending { it.changeFromLast2Weeks }.take(50))
    printTable(maxDate, "1月内市值变化前50名", x.sortedByDescending { it.changeFromLastMonth }.take(50))
    printTable(maxDate, "1天内市值变化后50名", x.sortedBy { it.changeFromLastDay }.take(50))
    printTable(maxDate, "1周内市值变化后50名", x.sortedBy { it.changeFromLastWeek }.take(50))
    printTable(maxDate, "2周内市值变化后50名", x.sortedBy { it.changeFromLast2Weeks }.take(50))
    printTable(maxDate, "1月内市值变化后50名", x.sortedBy { it.changeFromLastMonth }.take(50))
    printTable(maxDate, "市值持续增加", x.filter {
        it.weightNow > it.weightLastDay && it.weightLastDay > it.weightLastWeek && it.weightNow > it.weightLast2Weeks && it.weightNow > it.weightLastMonth
    }.sortedByDescending { it.changeFromLastWeek })

//    printTable(maxDate, "市值持续加速增加", x.filter {
//        val c1 = it.changeFromLastDay
//        val c2 = (it.changeFromLastWeek - it.changeFromLastDay) / 6
//        val c3 = (it.changeFromLast2Weeks - it.changeFromLastWeek) / 7
//        val c4 = (it.changeFromLastMonth - it.changeFromLast2Weeks) / 15
//        c1 > c2 && c2 > c3 && c3 > c4 && c4 >= 0
//    }.sortedByDescending { it.changeFromLastMonth })

}

fun printTable(maxDate: String, title: String, rows: List<Row>) {
    println("-------------------------------------- $title ($maxDate) -------------------------------------")
    println("%30s\t%12s\t%12s\t%12s\t%12s\t%12s".format("Coin", "当前占有率", "1天内变化", "1周内变化", "2周内变化", "1月内变化"))
    rows.forEach {
        println("%30s\t%12.2f%%\t%12.2f%%\t%12.2f%%\t%12.2f%%\t%12.2f%%".format("(${it.coin.name}) ${it.coin.symbol}", it.weightNow * 100, it.changeFromLastDay * 100, it.changeFromLastWeek * 100, it.changeFromLast2Weeks * 100, it.changeFromLastMonth * 100))
    }
    println()
}

data class Row(val coin: Coin, val weightNow: Double, val weightLastDay: Double, val weightLastWeek: Double, val weightLast2Weeks: Double, val weightLastMonth: Double) {
    val changeFromLastDay: Double = weightNow - weightLastDay
    val changeFromLastWeek: Double = weightNow - weightLastWeek
    val changeFromLast2Weeks: Double = weightNow - weightLast2Weeks
    val changeFromLastMonth: Double = weightNow - weightLastMonth
}

data class WeightOfDate(val date: String, val totalMarketCap: Double, val weights: Map<Coin, Double>)

fun weightOfDate(day: String, coinsData: Map<Coin, List<HistoryData>>): WeightOfDate {
    val marketData = coinsData.mapValues {
        val cap = it.value.find { it.date == day }?.marketCap
        if (cap == "-") 0.0 else cap?.toDouble()
    }.removeNullValues()

    val totalMarketCap = marketData.values.sum()
    return WeightOfDate(day, totalMarketCap, marketData.mapValues { it.value / totalMarketCap })
}

fun <K, V> Map<K, V?>.removeNullValues(): Map<K, V> {
    return mapNotNull { (key, nullableValue) ->
        nullableValue?.let { key to it }
    }.toMap()
}

private fun getTheDayBefore(day: String, daysBefore: Int): String {
    return LocalDate.parse(day).minusDays(daysBefore.toLong()).toString()
}
