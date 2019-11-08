package com.example.aniscoreandroid;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aniscoreandroid.model.BangumiBrief;
import com.example.aniscoreandroid.model.BangumiListData;
import com.example.aniscoreandroid.model.BangumiListResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    Retrofit retrofit = new Retrofit.Builder().baseUrl("http://10.0.2.2:4000").
            addConverterFactory(GsonConverterFactory.create()).build();
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.rank_list);
        Fragment fragment = new Home();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, fragment);
        ft.commit();
    }

    private void getRank() {
        ServerCall service = retrofit.create(ServerCall.class);
        Call<BangumiListResponse> rankCall = service.getBangumiRank(3);
        rankCall.enqueue(new Callback<BangumiListResponse>() {
            @Override
            public void onResponse(Call<BangumiListResponse> call, Response<BangumiListResponse> response) {
                if (response.isSuccessful()) {
                    BangumiListData data = response.body().getData();
                    List<BangumiBrief> bangumiList = data.getBangumiList();
                    recyclerView.setAdapter(new BangumiBriefAdapter(bangumiList));
                    recyclerView.setLayoutManager(new GridLayoutManager(getBaseContext(), 3));
                }
            }

            @Override
            public void onFailure(Call<BangumiListResponse> call, Throwable t) {
                System.out.println("fail");
            }
        });

    }
}
