package tv.freewheel.demo;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class FWConfigActivity extends Activity {
	private EditText networkIdEdit;
	private EditText adsUrlEdit;
	private EditText profileEdit;
	private EditText siteSectionIdEdit;
	private EditText videoAssetIdEdit;
	private EditText displayWidthEdit;
	private EditText displayHeightEdit;
	private EditText splashCompatibleDimensionsEdit;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.config);
		
		this.networkIdEdit = (EditText)this.findViewById(R.id.networkIdEdit);
		this.adsUrlEdit = (EditText)this.findViewById(R.id.adsUrlEdit);
		this.profileEdit = (EditText)this.findViewById(R.id.profileEdit);
		this.siteSectionIdEdit = (EditText)this.findViewById(R.id.siteSectionIdEdit);
		this.videoAssetIdEdit = (EditText)this.findViewById(R.id.videoAssetIdEdit);
		this.displayWidthEdit = (EditText)this.findViewById(R.id.displayWidthEdit);
		this.displayHeightEdit = (EditText)this.findViewById(R.id.displayHeightEdit);
		this.splashCompatibleDimensionsEdit = (EditText)this.findViewById(R.id.splashCompatibleDimensionsEdit);
	}
	
	private void loadConfig() {
		this.networkIdEdit.setText(String.valueOf(FWConfig.networkId), TextView.BufferType.EDITABLE);
		this.adsUrlEdit.setText(FWConfig.adserverUrl, TextView.BufferType.EDITABLE);
		this.profileEdit.setText(FWConfig.profile, TextView.BufferType.EDITABLE);
		this.siteSectionIdEdit.setText(FWConfig.siteSectionId, TextView.BufferType.EDITABLE);
		this.videoAssetIdEdit.setText(FWConfig.videoAssetId, TextView.BufferType.EDITABLE);
		this.displayWidthEdit.setText(String.valueOf(FWConfig.displayWidth), TextView.BufferType.EDITABLE);
		this.displayHeightEdit.setText(String.valueOf(FWConfig.displayHeight), TextView.BufferType.EDITABLE);
	}
	
	private void saveConfig() {
		FWConfig.networkId = Integer.parseInt(this.networkIdEdit.getText().toString());
		FWConfig.adserverUrl = this.adsUrlEdit.getText().toString();
		FWConfig.profile = this.profileEdit.getText().toString();
		FWConfig.siteSectionId = this.siteSectionIdEdit.getText().toString();
		FWConfig.videoAssetId = this.videoAssetIdEdit.getText().toString();
		FWConfig.displayWidth = Integer.parseInt(this.displayWidthEdit.getText().toString());
		FWConfig.displayHeight = Integer.parseInt(this.displayHeightEdit.getText().toString());
		FWConfig.splashCompatibleDimensions = this.splashCompatibleDimensionsEdit.getText().toString();
	}
	
	public void onCancelClicked(View view) {
		Intent returnIntent = new Intent();
		setResult(RESULT_CANCELED, returnIntent); 
		this.finish();
	}
	
	public void onSaveClicked(View view) {
		this.saveConfig();
		Intent returnIntent = new Intent();
		setResult(RESULT_OK, returnIntent); 
		this.finish();
	}

	@Override
	protected void onStart() {
		super.onStart();
		this.loadConfig();
	}
}
