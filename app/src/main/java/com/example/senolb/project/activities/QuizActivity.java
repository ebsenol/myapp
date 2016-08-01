package com.example.senolb.project.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.example.senolb.project.R;
import com.example.senolb.project.normalmodegif.ApiInterface;
import com.example.senolb.project.movie.ApiInterfaceMovie;
import com.example.senolb.project.normalmodegif.Data;
import com.example.senolb.project.normalmodegif.JsonResponse;
import com.example.senolb.project.movie.JsonResponse2;
import com.example.senolb.project.movie.Result;
import com.example.senolb.project.easymodegif.ListInterface;

import butterknife.BindView;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class QuizActivity extends Activity {
    @BindView(R.id.imageViewGif) ImageView gifView;
    @BindView(R.id.answer_1) Button btnA;
    @BindView(R.id.answer_2) Button btnB;
    @BindView(R.id.answer_3) Button btnC;
    @BindView(R.id.progress) ProgressBar progressBar;
    @BindView(R.id.first_text) TextView mainText;

    public String prevUrl="";
    public String url ="";
    final public int total = 15;                    //total num of gifs to be shown
    public String[] titles = new String[total];     //to hold movie titles
    public int count = 0;                           //index of current movie
    public int answer = -1;
    private Handler mHandler = new Handler();
    public int trueCounter=0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        ButterKnife.bind(this);

        // call the movie api
        int num = 1+(int)(Math.random() * 100); // get the page number for api
        String page = num +"";

        ApiInterfaceMovie service = ApiInterfaceMovie.retrofit2.create(ApiInterfaceMovie.class);
        float vote = (float) 5.9;
        String genre = getIntent().getExtras().getString("genre");
        Call<JsonResponse2> movieList;
        if (genre.equals("3")){  // get drama
            movieList = service.getMovieWithGenre(18,"en","052ab3ed3f1f39a747fc24b817ee31e7",page,vote);
        }
        else if (genre.equals("2")){ // get animation
            movieList = service.getMovieWithGenre(16,"en","052ab3ed3f1f39a747fc24b817ee31e7",page,vote);
        }
        else if (genre.equals("1")){ // get action movies
            movieList = service.getMovieWithGenre(28,"en","052ab3ed3f1f39a747fc24b817ee31e7",page,vote);
        }
        else //default case
           movieList = service.getMovie("en","052ab3ed3f1f39a747fc24b817ee31e7",page,vote); // insert queries

        movieList.enqueue(new Callback<JsonResponse2>() {
            @Override
            public void onResponse(Call<JsonResponse2> call, Response<JsonResponse2> response) {
                if (response.isSuccessful()) { //got response
                    int i = 0;
                    for (Result result : response.body().getResults()) {
                        // remove the part after ":" and add to the array
                        if(result.getOriginalLanguage().equals("en")){
                            int ind = result.getOriginalTitle().indexOf(":");
                            if (ind != -1) {
                                String str = result.getOriginalTitle().substring(0, ind);
                                titles[i] = str;
                            } else {
                                titles[i] = result.getOriginalTitle();
                            }
                            i++;
                        }
                        if (i>total-1) break; // reached to number of total movies
                    }
                    request(getCurrentFocus()); // TODO gives graphical corruption error but runs , why?
                } else {
                    //unsuccessful response
                }
            }
            @Override
            public void onFailure(Call<JsonResponse2> call, Throwable t) {
                //display the error
                Log.d("Error", t.getMessage());
            }
        });
    }
    public void showName(View view){ //show name of the current gif
        if (count==0) // if there is no gif
            mainText.setText("--no title--");
        else
            mainText.setText(titles[count-1]);
    }

    public void request(View view) {
        btnA.setBackgroundResource(R.drawable.btn_normal); // make the buttons default color again
        btnB.setBackgroundResource(R.drawable.btn_normal);
        btnC.setBackgroundResource(R.drawable.btn_normal);

        if (count == total) { // go to main page if total count is reached
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else {
            //get the movie title from array
            String keyword = titles[count];
            answer = 1 + (int) (Math.random() * 3); //set the answer
            switch (answer) {
                case 1:
                    fillContent(btnB, btnC, btnA, keyword);
                    break;
                case 2:
                    fillContent(btnA, btnC, btnB, keyword);
                    break;
                case 3:
                    fillContent(btnA, btnB, btnC, keyword);
                default:
                    break;
            }

            keyword = keyword + " movie";
            mainText.setText("True counter: " + trueCounter + "/" + count);
            count++;

            if( !getIntent().getExtras().getBoolean("easyMode")) { //normal mode
                //call gif api
                ApiInterface service = ApiInterface.retrofit.create(ApiInterface.class);
                Call<JsonResponse> myDownsized = service.getGif("dc6zaTOxFJmzC", "json", keyword);

                myDownsized.enqueue(new Callback<JsonResponse>() {
                    @Override
                    public void onResponse(Call<JsonResponse> call,Response<JsonResponse> response){
                        if (response.isSuccessful()) {
                            //get the data
                            Data data = response.body().getData();
                            prevUrl = url;
                            url = data.getImageOriginalUrl();
                            progressBar.setVisibility(View.VISIBLE); //start spinning
                            //display the gif
                            GlideDrawableImageViewTarget imageViewTarget =
                                    new GlideDrawableImageViewTarget(gifView);
                            Glide
                                    .with(getApplicationContext())
                                    .load(url)
                                    .error(R.drawable.bg)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .listener(new RequestListener<String, GlideDrawable>() {
                                        @Override
                                        public boolean onException(Exception e, String model,
                                                                   Target<GlideDrawable> target,
                                                                   boolean isFirstResource) {
                                            return false;
                                        }

                                        @Override
                                        public boolean onResourceReady(GlideDrawable resource,
                                                                       String model,
                                                                       Target<GlideDrawable> target,
                                                                       boolean isFromMemoryCache,
                                                                       boolean isFirstResource) {
                                            progressBar.setVisibility(View.INVISIBLE); //gif is ready
                                            btnA.setVisibility(View.VISIBLE);
                                            btnB.setVisibility(View.VISIBLE);
                                            btnC.setVisibility(View.VISIBLE);
                                            return false;
                                        }
                                    })
                                    .bitmapTransform(new RoundedCornersTransformation
                                                            (getApplicationContext(), 10, 10))
                                    .into(imageViewTarget);

                        } else { //unsuccessful response

                        }
                    }

                    @Override
                    public void onFailure(Call<JsonResponse> call, Throwable t) {
                        Log.d("Error", t.getMessage());
                    }
                }); // end of normal mode
            }
            else { //easy mode
                ListInterface service = ListInterface.retrofit.create(ListInterface.class);
                Call<com.example.senolb.project.easymodegif.JsonResponse> myDownsized =
                        service.getDownsized("dc6zaTOxFJmzC", "json", keyword,"3"); // api key, format, tag

                myDownsized.enqueue(new Callback<com.example.senolb.project.easymodegif.JsonResponse>(){
                    @Override
                    public void onResponse(Call<com.example.senolb.project.easymodegif.JsonResponse> call,
                                           Response<com.example.senolb.project.easymodegif.JsonResponse> response) {
                        if (response.isSuccessful()) {
                            //get the data
                            int n = (int)(Math.random() * 2);
                            com.example.senolb.project.easymodegif.Data data = response.body().getDataList().get(n);

                            url = data.getImages().getDownsized().getUrl();
                            progressBar.setVisibility(View.VISIBLE);
                            //display the gif
                            GlideDrawableImageViewTarget imageViewTarget = new GlideDrawableImageViewTarget(gifView);
                            Glide
                                    .with(getApplicationContext())
                                    .load(url)
                                    .error(R.drawable.bg)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .listener(new RequestListener<String, GlideDrawable>() {
                                        @Override
                                        public boolean onException(Exception e,
                                                                   String model,
                                                                   Target<GlideDrawable> target,
                                                                   boolean isFirstResource) {
                                            return false;
                                        }

                                        @Override
                                        public boolean onResourceReady(GlideDrawable resource,
                                                                       String model,
                                                                       Target<GlideDrawable> target,
                                                                       boolean isFromMemoryCache,
                                                                       boolean isFirstResource) {
                                            progressBar.setVisibility(View.INVISIBLE); //gif is ready
                                            btnA.setVisibility(View.VISIBLE);
                                            btnB.setVisibility(View.VISIBLE);
                                            btnC.setVisibility(View.VISIBLE);
                                            return false;
                                        }
                                    })
                                    .bitmapTransform(new RoundedCornersTransformation
                                                                    (getApplicationContext(),10,10))
                                    .into(imageViewTarget);

                        } else { //unsuccessful response

                        }
                    }

                    @Override
                    public void onFailure(Call<com.example.senolb.project.easymodegif.JsonResponse> call,
                                          Throwable t) {
                        Log.d("Error", t.getMessage());
                    }
                });
            }
        }
    }


    public void check(View view) { // checks if the answer is true or false
        if ( answer == 1 && btnA == view){
            trueAnswer(btnA);
        }
        else if ( answer == 2 && btnB == view){
            trueAnswer(btnB);
        }
        else if ( answer == 3 && btnC == view){
            trueAnswer(btnC);
        }
        else if ( answer == -1 ){ //first click
            request(view);
        }
        else falseAnswer(view);


    }
    public Button getAnswer(){
        if (answer == 1)
            return btnA;
        else if (answer == 2)
            return btnB;
        else if (answer == 3)
            return btnC;
        else return null;

    }

    public void trueAnswer(final View view) {
        trueCounter++;
        view.setBackgroundResource(R.drawable.btn_true);

        mHandler.postDelayed(new Runnable() {
            public void run() {
                request(view);
            }
        }, 1500);
    }

    public void falseAnswer(final View view){
        view.setBackgroundResource(R.drawable.btn_false);
        getAnswer().setBackgroundResource(R.drawable.btn_true);

        mHandler.postDelayed(new Runnable() {
            public void run() {
                request(view);
            }
        }, 1500);
    }

    public void fillContent(final Button b1, final Button b2, final Button trueButton, final String keyword){

        int num = 1+(int)(Math.random() * 100);
        String n = num +"";

        ApiInterfaceMovie s = ApiInterfaceMovie.retrofit2.create(ApiInterfaceMovie.class);
        Call<JsonResponse2> movieList = s.getMovie("en","052ab3ed3f1f39a747fc24b817ee31e7",n,(float)4.5);
        movieList.enqueue(new Callback<JsonResponse2>() {
            @Override
            public void onResponse(Call<JsonResponse2> call, Response<JsonResponse2> response) {
                if (response.isSuccessful()) { //got response
                    int i = 0;
                    int num = 1+(int)(Math.random() * 15);
                    if(!response.body().getResults().get(num).getOriginalLanguage().equals("en"))
                        num = 1+(int)(Math.random() * 15);
                    int num2 = 1+(int)(Math.random() * 15);
                    if (num==num2 || !response.body().getResults().get(num2).getOriginalLanguage().equals("en"))
                        num2 = 1+(int)(Math.random() * 15);
                    b1.setVisibility(View.INVISIBLE);
                    b2.setVisibility(View.INVISIBLE);
                    trueButton.setVisibility(View.INVISIBLE);
                    b1.setText(response.body().getResults().get(num).getOriginalTitle());
                    b2.setText(response.body().getResults().get(num2).getOriginalTitle());
                    trueButton.setText(keyword);
                }
             else {
                    //unsuccessful response
                }
            }
            @Override
            public void onFailure(Call<JsonResponse2> call, Throwable t) {
                //display the error
                Log.d("Error", t.getMessage());
            }
        });
    }
}
