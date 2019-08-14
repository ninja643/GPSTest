package rs.ac.ni.pmf.gpstest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity
{
	private static final int REQUEST_CODE_ACCESS_LOCATION = 1;
	private static final int REQUEST_CHECK_SETTINGS = 2;
	
	public static final String LOCATION_TAG = "LOCATION";

	private FusedLocationProviderClient _locationProviderClient;
	private LocationRequest _locationRequest = LocationRequest.create();
	private LocationCallback _locationCallback;

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		_locationProviderClient = LocationServices.getFusedLocationProviderClient(this);

		_locationCallback = new LocationCallback()
		{
			@Override
			public void onLocationAvailability(final LocationAvailability locationAvailability)
			{
				Log.i(LOCATION_TAG, "Availability: " + locationAvailability.toString());
				super.onLocationAvailability(locationAvailability);
			}

			@Override
			public void onLocationResult(final LocationResult locationResult)
			{
				handleLocationUpdate(locationResult);
			}
		};

		final int checkFinePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
		final int checkCoarsePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

		if (checkFinePermission != PackageManager.PERMISSION_GRANTED && checkCoarsePermission != PackageManager.PERMISSION_GRANTED)
		{
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
					REQUEST_CODE_ACCESS_LOCATION);
		}
		else
		{
			createLocationRequest();
		}
	}

	private void handleLocationUpdate(final LocationResult locationResult)
	{
		if (locationResult == null)
		{
			Log.i(LOCATION_TAG, "Got null location result");
			return;
		}

		for (final Location location : locationResult.getLocations())
		{
			handleSingleLocation(location);
		}
	}

	private void handleSingleLocation(final Location location)
	{
		Log.i(LOCATION_TAG, location.toString());
	}

	@Override
	public void onRequestPermissionsResult(
			final int requestCode,
			@android.support.annotation.NonNull final String[] permissions,
			@android.support.annotation.NonNull final int[] grantResults)
	{
		switch (requestCode)
		{
			case REQUEST_CODE_ACCESS_LOCATION:
				// We assume that user gave the permission
				createLocationRequest();
				break;

			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	private void createLocationRequest()
	{
		_locationRequest
				.setInterval(10000)
				.setFastestInterval(5000)
				.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		final LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
				.addLocationRequest(_locationRequest);

		final SettingsClient settingsClient = LocationServices.getSettingsClient(this);
		final Task<LocationSettingsResponse> locationSettingsResponseTask =
				settingsClient.checkLocationSettings(builder.build());

		locationSettingsResponseTask.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>()
		{
			@Override
			public void onSuccess(final LocationSettingsResponse locationSettingsResponse)
			{
				startLocationUpdates();
			}
		}).addOnFailureListener(this, new OnFailureListener()
		{
			@Override
			public void onFailure(@NonNull final Exception e)
			{
				if (e instanceof ResolvableApiException)
				{
					try
					{
						final ResolvableApiException resolvableApiException = (ResolvableApiException) e;
						resolvableApiException.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
					}
					catch (final IntentSender.SendIntentException sendException)
					{
						// Ignore this error
					}
				}
			}
		});
	}

	@SuppressLint("MissingPermission")
	private void startLocationUpdates()
	{
		_locationProviderClient.requestLocationUpdates(_locationRequest, _locationCallback, null);
	}
}
