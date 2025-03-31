package com.example.forecaservicesecond

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class WeatherActivity : AppCompatActivity() {

    //переменные====================================================================================
    private val forecaBaseUrl = Constants.FORECA_URL
    private var token = Constants.FORECA_TOKEN
    private val retrofit = Retrofit.Builder()
        .baseUrl(forecaBaseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(
            RxJava2CallAdapterFactory.create()
        )
        .build()

    private val forecaService = retrofit.create(ForecaApi::class.java)

    private val locations = ArrayList<ForecastLocation>()

    private val adapter = LocationsAdapter {
        showWeather(it)
    }

    private lateinit var searchButton: Button
    private lateinit var queryInput: EditText
    private lateinit var placeholderMessage: TextView
    private lateinit var locationsList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_weather)

        placeholderMessage = findViewById(R.id.placeholderMessage)
        searchButton = findViewById(R.id.searchButton)
        queryInput = findViewById(R.id.queryInput)
        locationsList = findViewById(R.id.locations)

        adapter.locations = locations

        locationsList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        locationsList.adapter = adapter

        searchButton.setOnClickListener{
            //if (queryInput.text.isNotEmpty()) {
                if (token.isEmpty()) {
                    authenticate()
                    Log.d("RxJava", "authenticate activated")
                } else {
                    search(token, Constants.HARDCODED_LOCATION)
                }
            //}
        }
    }

    private fun showWeather(location: ForecastLocation) {
        forecaService.getForecast("Bearer $token", location.id)
            .enqueue(object : Callback<ForecastResponse> {
                override fun onResponse(call: Call<ForecastResponse>,
                                        response: Response<ForecastResponse>) {
                    if (response.body()?.current != null) {
                        val message = "${location.name} t: ${response.body()?.current?.temperature}\n(Ощущается как ${response.body()?.current?.feelsLikeTemp})"
                        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                }

            })
    }

    private fun showMessage(text: String, additionalMessage: String) {
        if (text.isNotEmpty()) {
            placeholderMessage.visibility = View.VISIBLE
            locations.clear()
            adapter.notifyDataSetChanged()
            placeholderMessage.text = text
            if (additionalMessage.isNotEmpty()) {
                Toast.makeText(applicationContext, additionalMessage, Toast.LENGTH_LONG)
                    .show()
            }
        } else {
            placeholderMessage.visibility = View.GONE
        }
    }


    @SuppressLint("CheckResult")
    private fun authenticate() {
        forecaService.authenticate(ForecaAuthRequest(Constants.FORECA_USER, Constants.FORECA_PASSWORD))
            .flatMap { tokenResponse ->
                token = tokenResponse.token

                val bearerToken = "Bearer ${tokenResponse.token}"
                forecaService.getLocations(bearerToken, Constants.HARDCODED_LOCATION)
            }.retry { count, throwable ->
                count < 3 && throwable is HttpException && throwable.code() == 401
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { locationsResponse ->
                    Log.d("RxJava", "Got locations: ${locationsResponse.locations}")
                },
                { error ->
                    Log.e("RxJava", "Got error with auth or locations", error)
                }
            )
            /*
            .enqueue(object : Callback<ForecaAuthResponse> {
                override fun onResponse(call: Call<ForecaAuthResponse>,
                                        response: Response<ForecaAuthResponse>) {
                    if (response.code() == 200) {
                        token = response.body()?.token.toString()
                        search(token, Constants.HARDCODED_LOCATION)
                    } else {
                        Log.e("RxJavaForeca", "Something went wrong with auth: ${response.code().toString()}")
                        showMessage(getString(R.string.something_went_wrong), response.code().toString())
                    }
                }

                override fun onFailure(call: Call<ForecaAuthResponse>, t: Throwable) {
                    Log.e("RxJavaForeca", "onFailure auth request", t)
                    showMessage(getString(R.string.something_went_wrong), t.message.toString())
                }

            })
             */

    }

    @SuppressLint("CheckResult")
    private fun search(accessToken: String, searchQuery: String) {
        forecaService.getLocations("Bearer $accessToken", searchQuery)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {locations ->

                },
                {error ->

                }
            )
            /*
            .enqueue(object : Callback<LocationsResponse> {
                override fun onResponse(call: Call<LocationsResponse>,
                                        response: Response<LocationsResponse>) {
                    when (response.code()) {
                        200 -> {
                            if (response.body()?.locations?.isNotEmpty() == true) {
                                locations.clear()
                                locations.addAll(response.body()?.locations!!)
                                adapter.notifyDataSetChanged()
                                showMessage("", "")
                                Log.d("RxJavaForeca", "Found locations!")
                                locations.forEach {
                                    Log.d("RxJavaForeca", it.toString())
                                }
                            } else {
                                showMessage(getString(R.string.nothing_found), "")
                                Log.d("RxJavaForeca", "Nothing found")
                            }

                        }
                        401 -> authenticate()
                        else -> {
                            showMessage(getString(R.string.something_went_wrong), response.code().toString())
                            Log.e("RxJavaForeca", "Something went wrong with search: ${response.code().toString()}")
                        }
                    }



                }

                override fun onFailure(call: Call<LocationsResponse>, t: Throwable) {
                    showMessage(getString(R.string.something_went_wrong), t.message.toString())
                    Log.e("RxJavaForeca", "onFailure search request", t)
                }

            })

             */
    }


}