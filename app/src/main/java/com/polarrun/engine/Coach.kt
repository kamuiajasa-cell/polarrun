package com.polarrun.engine

import com.polarrun.data.Journal
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

data class Advice(
    val kind: Kind,
    val totalMin: Int,
    val reps: Int,
    val workSec: Int,
    val restSec: Int,
    val message: String,
    val adjusted: Boolean
)

object Coach {
    fun advise(p: Session?, score: Int?, j: Journal?): Advice {
        if (p == null) {
            return Advice(
                Kind.REST, 0, 0, 0, 0,
                "Tidak ada jadwal lari hari ini. Pemulihan juga bagian dari latihan.", false
            )
        }
        if (j != null && j.redFlag) {
            return Advice(
                Kind.REST, 0, 0, 0, 0,
                "Kamu menandai ada tanda bahaya atau nyeri cedera, jadi hari ini istirahat dulu. " +
                    "Kalau nyerinya masih ada 2-3 hari lagi, sebaiknya periksa ke fisioterapis atau dokter.",
                true
            )
        }
        if (score == null) {
            return Advice(
                p.kind, p.totalMin, p.reps, p.workSec, p.restSec,
                "Jurnal hari ini belum diisi, jadi sesi tampil sesuai rencana. " +
                    "Isi Jurnal supaya coach bisa menyesuaikan.",
                false
            )
        }
        return when (p.kind) {
            Kind.HARD -> when {
                score >= 60 -> Advice(
                    p.kind, p.totalMin, p.reps, p.workSec, p.restSec,
                    "Kondisi bagus. Jalankan sesi keras sesuai rencana dan jaga rep terakhir tetap terkendali.",
                    false
                )
                score >= 40 -> {
                    val r2 = max(2, ceil(p.reps * 0.6).toInt())
                    val cut = ceil((p.reps - r2) * (p.workSec + p.restSec) / 60.0).toInt()
                    Advice(
                        Kind.HARD, max(20, p.totalMin - cut), r2, p.workSec, p.restSec,
                        "Kesiapan sedang. Sesi keras tetap jalan, tapi repetisinya dikurangi jadi $r2. " +
                            "Kualitas lebih penting daripada jumlah.",
                        true
                    )
                }
                else -> Advice(
                    Kind.EASY, max(20, (p.totalMin * 0.8).roundToInt()), 0, 0, 0,
                    "Kesiapan rendah. Sesi keras diganti easy hari ini supaya tubuh pulih. " +
                        "Sesi keras tidak hilang, cukup digeser ke jadwal berikutnya.",
                    true
                )
            }
            else -> when {
                score >= 40 -> Advice(
                    p.kind, p.totalMin, 0, 0, 0,
                    "Aman untuk lanjut sesuai rencana. Jaga tetap santai di zona easy.",
                    false
                )
                score >= 25 -> Advice(
                    p.kind, max(20, (p.totalMin * 0.75).roundToInt()), 0, 0, 0,
                    "Kesiapan agak rendah. Durasi dipotong 25% dan tetap di zona easy.",
                    true
                )
                else -> Advice(
                    p.kind, max(15, (p.totalMin * 0.5).roundToInt()), 0, 0, 0,
                    "Kesiapan rendah. Cukup sesi singkat yang sangat santai, atau istirahat kalau badan minta.",
                    true
                )
            }
        }
    }
}
