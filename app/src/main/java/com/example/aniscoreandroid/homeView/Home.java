package com.example.aniscoreandroid.homeView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aniscoreandroid.R;
import com.example.aniscoreandroid.utils.ServerCall;
import com.example.aniscoreandroid.adapter.BangumiBriefAdapter;
import com.example.aniscoreandroid.adapter.BangumiBriefScoreAdapter;
import com.example.aniscoreandroid.model.bangumiList.BangumiBrief;
import com.example.aniscoreandroid.model.bangumiListScore.BangumiBriefScore;
import com.example.aniscoreandroid.model.bangumiList.BangumiListData;
import com.example.aniscoreandroid.model.bangumiList.BangumiListResponse;
import com.example.aniscoreandroid.model.bangumiListScore.BangumiListScoreData;
import com.example.aniscoreandroid.model.bangumiListScore.BangumiListScoreResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Home extends Fragment {
    private RecyclerView rankView;
    private RecyclerView currentBangumiView;
    private RecyclerView previousBangumiView;
    private RecyclerView beforePreviousBangumiView;
    private View view;
    private BottomNavigationView navigationView;
    private final int numThread = 4;
    Retrofit retrofit = new Retrofit.Builder().baseUrl("http://10.0.2.2:4000").
            addConverterFactory(GsonConverterFactory.create()).build();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        view = inflater.inflate(R.layout.home_view, container, false);
        view.setVisibility(View.GONE);
        rankView = view.findViewById(R.id.rank_list);
        currentBangumiView = view.findViewById(R.id.current_bangumi_list);
        previousBangumiView = view.findViewById(R.id.previous_bangumi_list);
        beforePreviousBangumiView = view.findViewById(R.id.before_previous_bangumi_list);
        navigationView = getActivity().findViewById(R.id.navigation);
        loadBangumi();
        setRankListener();
        setSeasonListener();
        return view;
    }

    /*
     * concurrently call endpoint using thread pool
     */
    private void loadBangumi() {
        ExecutorService exec = Executors.newFixedThreadPool(numThread);
        exec.execute(new Runnable() {
            @Override
            public void run() {
                getRank();
            }
        });
        exec.execute(new Runnable() {
            @Override
            public void run() {
                getCurrentBangumiView();
            }
        });
        exec.execute(new Runnable() {
            @Override
            public void run() {
                getPreviousBangumiView();
            }
        });
        exec.execute(new Runnable() {
            @Override
            public void run() {
                getBeforePreviousBangumiView();
            }
        });
        exec.shutdown();
    }

    /*
     * get top 3 bangumi with highest score
     */
    private void getRank() {
        ServerCall service = retrofit.create(ServerCall.class);
        Call<BangumiListScoreResponse> rankCall = service.getBangumiRank(3);
        rankCall.enqueue(new Callback<BangumiListScoreResponse>() {
            @Override
            public void onResponse(Call<BangumiListScoreResponse> call, Response<BangumiListScoreResponse> response) {
                if (response.isSuccessful()) {
                    view.setVisibility(View.VISIBLE);
                    BangumiListScoreData data = response.body().getData();
                    List<BangumiBriefScore> bangumiList = data.getBangumiList();
                    rankView.setAdapter(new BangumiBriefScoreAdapter(bangumiList));
                    rankView.setLayoutManager(new GridLayoutManager(getContext(), 3));
                }
            }

            @Override
            public void onFailure(Call<BangumiListScoreResponse> call, Throwable t) {
                System.out.println("fail");
            }
        });
    }

    /*
     * click the text and switch to the rank fragment
     */
    private void setRankListener() {
        TextView toRank = view.findViewById(R.id.to_rank);
        toRank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new Rank();
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.container, fragment);
                navigationView.setSelectedItemId(navigationView.getMenu().getItem(2).getItemId());
                ft.commit();
            }
        });
    }

    /*
     * click the text and switch to the season fragment
     */
    private void setSeasonListener() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new Season();
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.container, fragment);
                navigationView.setSelectedItemId(navigationView.getMenu().getItem(1).getItemId());
                ft.commit();
            }
        };
        TextView toSeason1 = view.findViewById(R.id.to_season_1);
        TextView toSeason2 = view.findViewById(R.id.to_season_2);
        TextView toSeason3 = view.findViewById(R.id.to_season_3);
        toSeason1.setOnClickListener(listener);
        toSeason2.setOnClickListener(listener);
        toSeason3.setOnClickListener(listener);
    }

    /*
     * get current season bangumi
     */
    private void getCurrentBangumiView() {
        String[] time = getCurrentSeason();
        setDateTitle(time[2], R.id.current_season_title);
        getBangumiOfYearSeason(time[0], time[1], currentBangumiView);
    }

    /*
     * get previous season anime
     */
    private void getPreviousBangumiView() {
        String[] time = getCurrentSeason();
        time = getPreviousSeason(time);
        setDateTitle(time[2], R.id.previous_season_title);
        getBangumiOfYearSeason(time[0], time[1], previousBangumiView);
    }

    /*
     * get bangumi of season before previous season
     */
    private void getBeforePreviousBangumiView() {
        String[] time = getCurrentSeason();
        time = getPreviousSeason(time);
        time = getPreviousSeason(time);
        setDateTitle(time[2], R.id.before_previous_season_title);
        getBangumiOfYearSeason(time[0], time[1], beforePreviousBangumiView);
    }

    /*
     * get first 3 bangumis of given year and season, and set to recyclerView
     */
    private void getBangumiOfYearSeason(String year, String season, final RecyclerView recyclerView) {
        ServerCall service = retrofit.create(ServerCall.class);
        Call<BangumiListResponse> getBangumiCall = service.getBangumiOfYearSeasonLimit(year, season);
        getBangumiCall.enqueue(new Callback<BangumiListResponse>() {
            @Override
            public void onResponse(Call<BangumiListResponse> call, Response<BangumiListResponse> response) {
                if (response.isSuccessful()) {
                    view.setVisibility(View.VISIBLE);
                    BangumiListData data = response.body().getData();
                    List<BangumiBrief> bangumiList = data.getBangumiList();
                    if (bangumiList.size() > 3) {
                        List<BangumiBrief> firstThreeBangumi = new ArrayList<>();
                        firstThreeBangumi.add(bangumiList.get(0));
                        firstThreeBangumi.add(bangumiList.get(1));
                        firstThreeBangumi.add(bangumiList.get(2));
                        recyclerView.setAdapter(new BangumiBriefAdapter(firstThreeBangumi));
                    } else {
                        recyclerView.setAdapter(new BangumiBriefAdapter(bangumiList));
                    }
                    recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
                }
            }

            @Override
            public void onFailure(Call<BangumiListResponse> call, Throwable t) {
                System.out.println("fail");
            }
        });
    }


    /*
     * get the season depend on current date
     */
    private String[] getCurrentSeason() {
        Date date = new Date();
        String[] time = new String[3];      // time[0] is year, time[1] is season
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        time[0] = year + "";
        if (month >= 1 && month < 4) {
            time[1] = "winter";
            time[2] = "1";
        } else if (month >= 4 && month < 7) {
            time[1] = "spring";
            time[2] = "4";
        } else if (month >= 7 && month <= 10) {
            time[1] = "summer";
            time[2] = "7";
        } else {
            time[1] = "fall";
            time[2] = "10";
        }
        return time;
    }

    /*
     * get previous season
     */
    private String[] getPreviousSeason(String[] time) {
        if (time[2].equals("1")) {         // current month is 1
            time[0] = Integer.parseInt(time[0]) - 1 + "";
        }
        if (time[1].equals("winter")) {
            time[1] = "fall";
            time[2] = "10";
        } else if (time[1].equals("spring")) {
            time[1] = "winter";
            time[2] = "1";
        } else if (time[1].equals("summer")) {
            time[1] = "spring";
            time[2] = "4";
        } else {
            time[1] = "summer";
            time[2] = "7";
        }
        return time;
    }

    /*
     * set title of date for season section given month and R.id
     */
    private void setDateTitle(String month, int id) {
        TextView title = view.findViewById(id);
        if (id == R.id.current_season_title) {
            title.setText((month + "月新番"));
        } else {
            title.setText((month + "月番"));
        }
    }
}