package com.nucleustech.mymentor.volley;

import org.json.JSONObject;

public interface ServerResponseCallback {

	/**
	 * Successful response callback
	 * */
	public void onSuccess(JSONObject resultJsonObject);

	/**
	 * Callback on Failure
	 * */
	public void onError();

}
