package temple.edu.bookcase;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class BookSearchActivity extends AppCompatActivity {

    EditText searchBox;
    Button searchButton;
    BookList searchedBooksFound;

    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_search);

        searchBox    = findViewById(R.id.editTextSearch);
        searchButton = findViewById(R.id.buttonSearch);
        searchedBooksFound = new BookList(new ArrayList<>());

        requestQueue = Volley.newRequestQueue(this);//set up volley

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String urlstr = "https://kamorris.com/lab/cis3515/search.php?term=" + searchBox.getText().toString();
                //We are using arrays for the ONLY purpose being that this is an array of JSONobjects
                JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, urlstr, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        for(int i = 0; i < response.length(); i++) { //we are returned an array from responses
                            try {
                                JSONObject holder = response.getJSONObject(i);
                                searchedBooksFound.getLibrary()
                                        .add(new Book(//parse the array by iterating through the returned search matches,
                                                Integer.parseInt(holder.getString("id")),
                                                holder.getInt("duration"),
                                                holder.getString("title"),
                                                holder.getString("author"),
                                                holder.getString("cover_url")));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Intent retIntent = new Intent();
                        retIntent.putExtra("BOOK_LIST", searchedBooksFound); //send back the info with correct keys and codes
                        setResult(12, retIntent);
                        finish();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(BookSearchActivity.this, "Unable to fulfill your request. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });

                requestQueue.add(jsonArrayRequest);
            }
        });
    }
}