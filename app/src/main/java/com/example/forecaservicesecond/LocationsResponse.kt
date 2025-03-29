package com.example.forecaservicesecond

import com.google.gson.annotations.SerializedName

class LocationsResponse(
    @SerializedName("locations")
    val locations: ArrayList<ForecastLocation>
)