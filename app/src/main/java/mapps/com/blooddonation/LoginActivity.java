package mapps.com.blooddonation;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    LinearLayout loginGroup;
    ProgressBar progressBar;
    EditText email, password, newPassword, confirmPassword;
    TextView heading, emailLabel, passwordLabel, newPassLabel, confirmPasswordLabel, nonFieldErrors;
    Button forgotPassButton, submitButton;

    RequestQueue queue;

    boolean changePass;
    boolean forgotPass = false;

    List<Pair<String, String>> keyLabels = new ArrayList<>();
    TextView labelViews[];

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = findViewById(R.id.login_toolbar);
        setSupportActionBar(toolbar);

        loginGroup = findViewById(R.id.login_group);
        progressBar = findViewById(R.id.toolbar_progress_bar);
        email = findViewById(R.id.email);
        passwordLabel = findViewById(R.id.password_label);
        password = findViewById(R.id.password);
        newPassword = findViewById(R.id.new_pass);

        emailLabel = findViewById(R.id.email_label);
        nonFieldErrors = findViewById(R.id.non_field_errors);

        forgotPassButton = findViewById(R.id.forgot_password);

        Bundle data = getIntent().getExtras();

        keyLabels.add(new Pair<>("email", "Email"));

        labelViews = new TextView[]{emailLabel, passwordLabel, nonFieldErrors};
        if (data != null) {
            changePass = true;

            heading = findViewById(R.id.heading);
            heading.setText("Change password");

            password.setHint("Enter current password");

            newPassLabel = findViewById(R.id.new_pass_label);
            newPassLabel.setVisibility(View.VISIBLE);

            newPassword.setVisibility(View.VISIBLE);

            confirmPasswordLabel = findViewById(R.id.cpassword_label);
            confirmPasswordLabel.setVisibility(View.VISIBLE);

            confirmPassword = findViewById(R.id.cpassword);
            confirmPassword.setVisibility(View.VISIBLE);

            submitButton = findViewById(R.id.submit);
            submitButton.setText("Change password");

            forgotPassButton.setVisibility(View.GONE);

            keyLabels.add(new Pair<>("password", "Current Password"));
            keyLabels.add(new Pair<>("new_password", "New Password"));
            labelViews = new TextView[]{emailLabel, passwordLabel, newPassLabel, nonFieldErrors};
        } else {
            keyLabels.add(new Pair<>("password", "Password"));
        }
        keyLabels.add(new Pair<>("non_field_errors", ""));

        queue = Volley.newRequestQueue(this);
    }

    public void submit(View view) {
        try {
            Helper.clearErrors(keyLabels, labelViews);
            String url = Helper.apiURL + "login";
            final String emailVal = email.getText().toString().trim(),
                    passwordVal = password.getText().toString(),
                    newPasswordVal = newPassword.getText().toString();

            JSONObject data = new JSONObject();
            data.put("email", emailVal);
            data.put("password", passwordVal);

            if (changePass) {
                final String confirmPasswordVal = confirmPassword.getText().toString();
                if (!newPasswordVal.equals(confirmPasswordVal)) {
                    Toast.makeText(getBaseContext(), "New password and confirm password do not match.", Toast.LENGTH_LONG).show();
                    return;
                }

                url = Helper.apiURL + "change_password";

                data.put("new_password", newPasswordVal);
            } else {
                data.put("password", passwordVal);
            }

            progressBar.setVisibility(View.VISIBLE);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    progressBar.setVisibility(View.GONE);
                    try {
                        if (response.getBoolean("status")) {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            if (changePass) {
                                intent.putExtra("password", newPasswordVal);
                                Toast.makeText(getBaseContext(), "Successfully changed the password.", Toast.LENGTH_LONG).show();
                            } else {
                                intent.putExtra("email", emailVal);
                                intent.putExtra("password", passwordVal);
                                Toast.makeText(getBaseContext(), "Successfully logged in.", Toast.LENGTH_LONG).show();
                            }
                            setResult(RESULT_OK, intent);
                            finish();
                        } else {
                            Helper.handleErrors(response, keyLabels, labelViews);
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getBaseContext(), "Error, check internet connection", Toast.LENGTH_LONG).show();
                }
            });

            queue.add(request);
        } catch (JSONException e) {
            Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            e.printStackTrace();
        }
    }

    @SuppressLint("SetTextI18n")
    public void forgotPassword(View view) {
        if (forgotPass) {
            progressBar.setVisibility(View.VISIBLE);
            try {
                JSONObject data = new JSONObject();
                data.put("email", email.getText().toString());

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, Helper.apiURL + "forgot_password", data, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressBar.setVisibility(View.GONE);
                        try {
                            if (response.getBoolean("status")) {
                                Toast.makeText(getBaseContext(), "New password sent to your email.", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(getBaseContext(), "Invalid email.", Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getBaseContext(), "Error, check internet connection", Toast.LENGTH_LONG).show();
                    }
                });
                queue.add(request);
            } catch (JSONException e) {
                Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
                e.printStackTrace();
            }
        } else {
            forgotPass = true;
            loginGroup.setVisibility(View.GONE);
            emailLabel.setText("Enter your email to get a new password.");
            forgotPassButton.setText("Submit");
        }
    }
}
