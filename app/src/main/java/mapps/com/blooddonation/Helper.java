package mapps.com.blooddonation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.Html;
import android.util.Pair;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

class Helper {

    final static String apiURL = "https://bd-backend-django.herokuapp.com/";

    static void handleErrors(JSONObject response, List<Pair<String, String>> keyLabels, TextView[] views) {
        try {
            JSONObject errors = response.getJSONObject("errors");
            JSONArray errorArray;

            StringBuilder errorList;
            for (int i = 0; i < keyLabels.size(); i++) {
                if (errors.has(keyLabels.get(i).first)) {
                    errorList = new StringBuilder();

                    errorArray = errors.getJSONArray(keyLabels.get(i).first);
                    for (int j = 0; j < errorArray.length(); j++) {
                        errorList.append("<br>");
                        errorList.append(errorArray.getString(j));
                    }
                    views[i].setText(Html.fromHtml(keyLabels.get(i).second + "<font color='red'>" + errorList + "</font>"), TextView.BufferType.SPANNABLE);
                }
            }
        } catch (JSONException e) {
            Toast.makeText(views[0].getContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    static void clearErrors(List<Pair<String, String>> keyLabels, TextView[] views) {
        for (int i = 0; i < keyLabels.size(); i++) {
            views[i].setText(keyLabels.get(i).second);
        }
    }

    static boolean isConnected(Context c) {
        ConnectivityManager
                cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null
                && activeNetwork.isConnectedOrConnecting();
    }
}
