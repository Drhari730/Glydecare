package com.diabeticcare.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GlydecareApi {
    @GET("health")
    suspend fun health(): Response<HealthResponse>

    @POST("api/patients")
    suspend fun upsertPatient(@Body request: PatientSyncRequest): Response<ApiOkResponse>

    @GET("api/doctor/dashboard")
    suspend fun dashboard(@Query("doctorId") doctorId: String): Response<DoctorDashboardResponse>

    @POST("api/patients/{patientId}/glucose")
    suspend fun addGlucose(
        @Path("patientId") patientId: String,
        @Body request: GlucoseSyncRequest
    ): Response<ApiOkResponse>
}

data class HealthResponse(
    val ok: Boolean = false,
    val service: String = "",
    val database: String = ""
)

data class ApiOkResponse(
    val ok: Boolean = false,
    val id: String? = null,
    val error: String? = null
)

data class PatientSyncRequest(
    val id: String,
    val doctorId: String,
    val name: String,
    val phone: String,
    val age: Int,
    val gender: String,
    val diabetesType: String,
    val diagnosisYear: Int,
    val doctorName: String,
    val lowGlucoseThreshold: Int,
    val highGlucoseThreshold: Int
)

data class GlucoseSyncRequest(
    val value: Int,
    val readingType: String,
    val notes: String,
    val timestamp: Long
)

data class DoctorDashboardResponse(
    val ok: Boolean = false,
    val doctorId: String = "",
    val patients: List<RemotePatient> = emptyList()
)

data class RemotePatient(
    val id: String = "",
    val doctorId: String = "",
    val name: String = "",
    val phone: String = "",
    val age: Int = 0,
    val gender: String = "",
    val diabetesType: String = "",
    val diagnosisYear: Int = 0,
    val doctorName: String = "",
    val glucose: Int = 0,
    val hba1c: Double = 0.0,
    val adherence: Int = 0
)
