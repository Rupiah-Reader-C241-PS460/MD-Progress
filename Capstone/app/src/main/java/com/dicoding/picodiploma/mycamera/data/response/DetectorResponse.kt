package com.dicoding.picodiploma.mycamera.data.response

import com.google.gson.annotations.SerializedName

data class DetectorResponse(

	@field:SerializedName("data")
	val data: Data,

	@field:SerializedName("message")
	val message: String,

	@field:SerializedName("status")
	val status: String
)

data class Data(

	@field:SerializedName("result")
	val result: String,

	@field:SerializedName("createdAt")
	val createdAt: String,

	@field:SerializedName("suggestion")
	val suggestion: String,

	@field:SerializedName("id")
	val id: String
)
