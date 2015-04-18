package com.example.skhalid.softmetersimulation;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class AlertDialogFragment extends DialogFragment {



	    public static AlertDialogFragment newInstance(String titleText, String bodyEText, String bodyText, String buttonText, int key) {
	    	AlertDialogFragment frag = new AlertDialogFragment();
	        Bundle args = new Bundle();
	        args.putString("titleText", titleText);
	        args.putString("bodyText", bodyText);
	        args.putString("bodyEText", bodyEText);
	        args.putString("buttonText", buttonText);
	        args.putInt("KEY", key);

	        frag.setArguments(args);
	        return frag;
	    }


		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			
			
	        String dialogTitleText = getArguments().getString("titleText");
	        String dialogBodyText = getArguments().getString("bodyText");
	        String dialogBodyEditText = getArguments().getString("bodyEText");
	        String dialogButtonText = getArguments().getString("buttonText");
	        
			LayoutInflater li = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = li.inflate(R.layout.dialog_layout, null);
	        

			 TextView dialogTitle = (TextView) v.findViewById(R.id.dialogTitle);
			 TextView dialogText = (TextView) v.findViewById(R.id.dialogText);
			 final EditText dialogEditText = (EditText) v.findViewById(R.id.dialogEditText);
			 if(!dialogBodyEditText.equalsIgnoreCase(""))
				 dialogEditText.setVisibility(View.VISIBLE);
			 Button dialogBtn = (Button) v.findViewById(R.id.dialogBtn1);

			dialogTitle.setText(dialogTitleText);
			dialogText.setText(dialogBodyText);
			dialogEditText.setText(dialogBodyEditText);
			dialogBtn.setText(dialogButtonText);
			
			dialogBtn.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					switch (getArguments().getInt("KEY")) {
//					case Constants.DOWNLOAD_APK_FILE:
//						((MainActivity) getActivity()).downloadApkFile();
//						break;
//
//					case Constants.SETTINGS:
//						if(Patterns.WEB_URL.matcher(dialogEditText.getText().toString()).matches()){
//						MainActivity.pref.edit().putString("WebServer", dialogEditText.getText().toString()).commit();
//						getDialog().dismiss();
//						}
//						else
//							Toast.makeText(getActivity(), "Please Enter Valid URL", Toast.LENGTH_LONG).show();
//						break;
//
					    case Constants.INFO:
						    getDialog().dismiss();
						    break;

                        case Constants.WARNING:
                            getDialog().dismiss();
                            getActivity().finish();
                            break;

                        case Constants.GPS:
                            Intent settingActivity = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            settingActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(settingActivity);
                            getDialog().dismiss();
                            break;
					default:
						break;
					}
					
				}
			});

            Dialog dialogBuilder;
            dialogBuilder = new Dialog(getActivity(), R.style.DialogSlideAnim);

            dialogBuilder.setContentView(v);
            dialogBuilder.setCancelable(false);
            dialogBuilder.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dialogBuilder.getWindow().setGravity(Gravity.BOTTOM);
            return dialogBuilder;
		}
	    
	    


}
