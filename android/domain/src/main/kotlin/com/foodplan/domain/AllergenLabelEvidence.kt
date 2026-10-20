package com.foodplan.domain

/**
 * Evidence schema for TR TS 022/2011 section 4.4(14).
 * These are regulated declarable components, not a medical diagnosis.
 * Sulphites have a regulatory threshold; aspartame requires a phenylalanine note.
 *
 * NO code path here claims "allergen free". Even a reviewed, unflagged label
 * might omit cross-contact or change between batches; confirm the actual pack.
 */
enum class DeclaredComponent {
    PEANUT, ASPARTAME_PHENYLALANINE, MUSTARD, SULPHITES_OVER_THRESHOLD,
    GLUTEN_CEREALS, SESAME, LUPIN, MOLLUSCS, MILK, TREE_NUTS,
    CRUSTACEANS, FISH, CELERY, SOY, EGGS
}

data class ProductLabelEvidence(
    /** The exact product offered; never inherit a different manufacturer's label. */
    val productUrl: String,
    val sku: String,
    val labelImageSource: String,
    val labelSha256: String,
    val reviewedBy: String,
    val capturedAtEpochMs: Long,
    val declaredComponents: Set<DeclaredComponent>,
    val possibleTraces: Set<DeclaredComponent>,
    /** Includes package composition AND the trace warning, even when absent. */
    val compositionAndTracesInspected: Boolean
)

enum class LabelDisclosureStatus {
    MISSING_OR_UNVERIFIED_LABEL, DECLARED_IN_INGREDIENTS,
    POSSIBLE_CROSS_CONTACT, NO_MATCHING_DECLARATION_ON_REVIEWED_LABEL
}
data class LabelDisclosureAssessment(
    val status: LabelDisclosureStatus,
    val matched: Set<DeclaredComponent>
) {
    /** A clear result is still NOT proof of clinical allergen safety. */
    val medicallyCertifiedSafe: Boolean get() = false
}

object AllergenLabelEvidence {
    const val MAX_LABEL_AGE_MS: Long = 30L * 24 * 60 * 60 * 1000
    const val REGULATION_URL: String =
        "https://www.consultant.ru/document/cons_doc_LAW_124614/69bd4f53fec865778fcae57126f17dc853f4fa81/"

    fun assess(
        productUrl: String,
        expectedSku: String,
        excluded: Set<DeclaredComponent>,
        label: ProductLabelEvidence?,
        nowEpochMs: Long
    ): LabelDisclosureAssessment {
        if (label == null || label.productUrl != productUrl ||
            expectedSku.isBlank() || expectedSku != expectedSku.trim() ||
            label.sku != expectedSku ||
            !productUrl.startsWith("https://") ||
            label.sku.isBlank() || label.reviewedBy.isBlank() ||
            !label.labelImageSource.startsWith("https://") ||
            !label.labelSha256.matches(Regex("[A-Fa-f0-9]{64}")) ||
            !label.compositionAndTracesInspected ||
            nowEpochMs < label.capturedAtEpochMs ||
            nowEpochMs - label.capturedAtEpochMs > MAX_LABEL_AGE_MS
        ) return LabelDisclosureAssessment(LabelDisclosureStatus.MISSING_OR_UNVERIFIED_LABEL,emptySet())

        val present = excluded.intersect(label.declaredComponents)
        if (present.isNotEmpty())
            return LabelDisclosureAssessment(LabelDisclosureStatus.DECLARED_IN_INGREDIENTS,present)
        val traces = excluded.intersect(label.possibleTraces)
        if (traces.isNotEmpty())
            return LabelDisclosureAssessment(LabelDisclosureStatus.POSSIBLE_CROSS_CONTACT,traces)
        return LabelDisclosureAssessment(
            LabelDisclosureStatus.NO_MATCHING_DECLARATION_ON_REVIEWED_LABEL,emptySet()
        )
    }
}
