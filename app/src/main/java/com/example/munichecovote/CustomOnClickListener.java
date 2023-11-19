package com.example.munichecovote;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONObject;

import java.util.Objects;

public class CustomOnClickListener implements GoogleMap.OnMarkerClickListener {
    private Context context;

    public CustomOnClickListener(Context context) {
        this.context = context;
    }
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Log.d("onMarker", "click");
        Log.d("onMarkerMarker", marker.toString());
        JSONObject tag = (JSONObject) marker.getTag();
        Log.d("onMarkerTag", tag.toString());

        parseMarkerTag(tag);
        return false;
    }

    private void parseMarkerTag(JSONObject tag) {
        Dialog dialogAddVote = new Dialog(context);
        dialogAddVote.setContentView(R.layout.layout_vote);
        dialogAddVote.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialogAddVote.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialogAddVote.getWindow().getAttributes().windowAnimations = R.style.animation;

        try {
            JSONObject props = tag.getJSONObject("properties");
            String type = props.getString("type");
            int votes = props.getInt("votes");
            int stars = props.getInt("stars");
            String descr = props.getString("description");

            if (type.equals("water fountain")) {
                TextView tvType = (TextView) dialogAddVote.findViewById(R.id.textViewMarkerType);
                tvType.setText("\uD83D\uDCA7");
                tvType = (TextView) dialogAddVote.findViewById(R.id.textType);
                tvType.setText("Water Fountain");
            } else if (type.equals("city green up")) {
                TextView tvType = (TextView) dialogAddVote.findViewById(R.id.textViewMarkerType);
                tvType.setText("\uD83C\uDF33");
                tvType = (TextView) dialogAddVote.findViewById(R.id.textType);
                tvType.setText("City Green Up");
            } else {
                TextView tvType = (TextView) dialogAddVote.findViewById(R.id.textViewMarkerType);
                tvType.setText("☂️");
                tvType = (TextView) dialogAddVote.findViewById(R.id.textType);
                tvType.setText("Public Roofing");
            }

            //TextView tvLoc = (TextView) dialogAddVote.findViewById(R.id.textViewLocation);
            //tvType.setText("Location");

            TextView tvVotesInt = (TextView) dialogAddVote.findViewById(R.id.textViewVotesInt);
            tvVotesInt.setText(Objects.toString(votes)+"\nVoters");

            RatingBar ratingBar = (RatingBar) dialogAddVote.findViewById(R.id.ratingBar);
            ratingBar.setRating(stars);

            // additional description
            TextView tvDescr = (TextView) dialogAddVote.findViewById(R.id.textViewDescription);
            tvDescr.setText(descr);

            //ImageView imgView = (ImageView) dialogAddVote.findViewById(R.id.imageView);
            //Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.waterglass_blue);
            //imgView.setImageBitmap(logo);

            //int progress = 10;
            ProgressBar progressBar = (ProgressBar) dialogAddVote.findViewById(R.id.progressBar4);
            progressBar.setProgress(Math.min(votes,5000));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        dialogAddVote.show();
        dialogAddVote.setCanceledOnTouchOutside(true);
    }
}