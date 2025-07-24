package com.chui.pos.dtos

import kotlinx.serialization.Serializable

@Serializable
data class BulkImportResponse(
    val totalRecords: Int,
    val successfulImports: Int,
    val failedImports: Int,
    val errors: List<String>
)

/**
 * Represents the validation status of a file selected for import.
 */
sealed class ImportValidationResult {
    object Idle : ImportValidationResult()
    object Valid : ImportValidationResult()
    data class Invalid(val reason: String) : ImportValidationResult()
}