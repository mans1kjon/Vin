package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NhtsaResult(
    val Value: String?,
    val Variable: String?,
    val VariableId: Int?,
    val ValueId: String?
)

@JsonClass(generateAdapter = true)
data class NhtsaResponse(
    val Count: Int,
    val Message: String,
    val SearchCriteria: String,
    val Results: List<NhtsaResult>
)

@JsonClass(generateAdapter = true)
data class RegistrationRecord(
    val period: String,
    val ownerType: String, // "Физическое лицо", "Юридическое лицо"
    val region: String,
    val isActive: Boolean
)

@JsonClass(generateAdapter = true)
data class AccidentRecord(
    val date: String,
    val type: String,
    val damagedParts: String,
    val region: String
)

@JsonClass(generateAdapter = true)
data class RestrictionRecord(
    val date: String,
    val basis: String, // e.g., "Постановление судебного пристава"
    val status: String, // e.g., "Активно"
    val region: String
)

@JsonClass(generateAdapter = true)
data class PledgeRecord(
    val pledgeDate: String,
    val pledgee: String, // Залогодержатель (Банк)
    val status: String
)

@JsonClass(generateAdapter = true)
data class FineRecord(
    val date: String,
    val description: String,
    val amount: Double,
    val isPaid: Boolean
)

@JsonClass(generateAdapter = true)
data class MileageRecord(
    val date: String,
    val valueKm: Int,
    val source: String
)

@JsonClass(generateAdapter = true)
data class VehicleReport(
    val vin: String,
    val plate: String?,
    val country: String, // "Россия" or "Таджикистан"
    val make: String,
    val model: String,
    val year: Int,
    val bodyType: String,
    val engineInfo: String,
    val color: String,
    val wmiCountry: String,
    val registrations: List<RegistrationRecord> = emptyList(),
    val accidents: List<AccidentRecord> = emptyList(),
    val restrictions: List<RestrictionRecord> = emptyList(),
    val pledges: List<PledgeRecord> = emptyList(),
    val fines: List<FineRecord> = emptyList(),
    val mileages: List<MileageRecord> = emptyList(),
    val isWanted: Boolean = false,
    val isFullReportPaid: Boolean = false
)

@Entity(tableName = "search_history")
data class SearchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val queryField: String, // VIN or plate number
    val queryType: String, // "VIN" or "PLATE"
    val country: String, // "Россия" or "Таджикистан"
    val make: String,
    val model: String,
    val year: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val jsonReport: String // Serialized VehicleReport
)
