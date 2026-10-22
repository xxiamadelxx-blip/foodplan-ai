package com.foodplan.domain

import kotlin.math.ceil

enum class PurchaseUnit { GRAM, PIECE, MILLILITRE }

data class PurchasePack(val unit: PurchaseUnit, val quantity: Int) {
    init { require(quantity > 0) }
}

data class UnitConversionEvidence(val gramsPerUnit: Double, val source: String) {
    init {
        require(gramsPerUnit.isFinite() && gramsPerUnit > 0)
        require(source.startsWith("https://"))
    }
}

object VerifiedPurchaseUnits {
    fun packs(requiredGrams: Int, pack: PurchasePack, evidence: UnitConversionEvidence? = null): Int? {
        require(requiredGrams >= 0)
        if (requiredGrams == 0) return 0
        val capacity = if (pack.unit == PurchaseUnit.GRAM) pack.quantity.toDouble()
            else pack.quantity * (evidence?.gramsPerUnit ?: return null)
        val count = ceil(requiredGrams / capacity)
        return if (count.isFinite() && count <= Int.MAX_VALUE) count.toInt() else null
    }
}
