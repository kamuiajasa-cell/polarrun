package com.polarrun.engine

object Zones {
    fun easyMaxHr(thr: Int): Int = (thr * 0.89).toInt()
    fun hardLoHr(thr: Int): Int = (thr * 0.95).toInt()
    fun hardHiHr(thr: Int): Int = (thr * 1.02).toInt()
}

fun mmss(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)
