package com.example.skhalid.softmetersimulation;

import android.util.Log;

import org.json.JSONObject;

import java.text.DecimalFormat;


public class DistanceCalculator {
	final static DecimalFormat decimalFormat = new DecimalFormat("0.00");
	private final static double Radius = 6377500; // earth's radius in meters (mean radius = 6,378km)

	public static double CalculateDistance(Double StartLat, Double StartLong, Double EndLat, Double EndLong) {
		double dLat = Math.toRadians(EndLat - StartLat);
		double dLon = Math.toRadians(EndLong - StartLong);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(StartLat)) * Math.cos(Math.toRadians(EndLat)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.asin(Math.sqrt(a));
		return (Radius * c) - Math.abs(0.0); //In meters
	}

}
