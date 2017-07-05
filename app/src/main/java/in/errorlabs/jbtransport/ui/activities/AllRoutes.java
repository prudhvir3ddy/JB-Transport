package in.errorlabs.jbtransport.ui.activities;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import net.steamcrafted.loadtoast.LoadToast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.errorlabs.jbtransport.R;
import in.errorlabs.jbtransport.ui.adapters.AllRoutesAdapter;
import in.errorlabs.jbtransport.ui.constants.RoutesSelectConstants;
import in.errorlabs.jbtransport.ui.models.RouteSelectModel;
import in.errorlabs.jbtransport.utils.Connection;
import in.errorlabs.jbtransport.utils.Constants;
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;
import okhttp3.OkHttpClient;

public class AllRoutes extends AppCompatActivity {
    OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();
    @BindView(R.id.routeselect_recyclerview) RecyclerView recyclerView;
    @BindView(R.id.r1) RelativeLayout nointernet;
    @BindView(R.id.searcherror) RelativeLayout searcherror;
    @BindView(R.id.rootView) RelativeLayout rootView;
    AllRoutesAdapter adapter;
    Connection connection;
    LoadToast loadToast;
    LinearLayoutManager layoutManager;
    List<RouteSelectModel> list = new ArrayList<>();
    String areaName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_routes);
        ButterKnife.bind(this);
        connection = new Connection(this);
        loadToast = new LoadToast(this);
        SlideInUpAnimator animator = new SlideInUpAnimator(new OvershootInterpolator(1f));
        recyclerView.setItemAnimator(animator);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        if (connection.isInternet()) {
            loadToast.show();
            try{
                Bundle bundle = getIntent().getExtras();
                areaName= bundle.getString("AreaName");
                Toast.makeText(getApplicationContext(),areaName,Toast.LENGTH_SHORT).show();
            }catch (Exception e){
                e.printStackTrace();
            }
            if (areaName!=null && areaName.length()>0){
                getAllRoutesData(areaName);
            }else {
                getAllRoutesData("0");
            }
        } else {
            recyclerView.setVisibility(View.GONE);
            nointernet.setVisibility(View.VISIBLE);
            Snackbar.make(nointernet, getString(R.string.nointernet), Snackbar.LENGTH_INDEFINITE).show();
        }

    }

    private void getAllRoutesData(String areaName){
        String Url;
        if (areaName.equals("0")){
            Url= Constants.RouteSelectDataUrl;
        }else {
            Url= Constants.SearchByName;
        }
        Toast.makeText(getApplicationContext(),Url,Toast.LENGTH_SHORT).show();
        AndroidNetworking.post(Url)
                .setPriority(Priority.HIGH)
                .addBodyParameter(Constants.AppKey, String.valueOf(R.string.transportAppKey))
                .addBodyParameter(Constants.AreaName, areaName)
                .setOkHttpClient(okHttpClient)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        loadToast.success();
                        Log.d("TAG", response.toString());
                        if (!response.has(getString(R.string.AuthError)) && !response.has("SearchErrorSelecting")) {
                            try {
                                JSONArray jsonArray = response.getJSONArray(getString(R.string.RoutesSelectJsonArrayName));
                                if (jsonArray.length() > 0) {
                                    int length = jsonArray.length();
                                    list.clear();
                                    for (int i = 0; i <= length; i++) {
                                        try {
                                            JSONObject object = jsonArray.getJSONObject(i);
                                            RouteSelectModel model = new RouteSelectModel();
                                            model.setRouteNumber(object.getString(RoutesSelectConstants.routeNumber));
                                            model.setFcmRouteID(object.getString(RoutesSelectConstants.fcmRouteId));
                                            model.setRouteFullPath(object.getString(RoutesSelectConstants.fullRoute));
                                            model.setRouteStartPoint(object.getString(RoutesSelectConstants.startPoint));
                                            model.setRouteEndPoint(object.getString(RoutesSelectConstants.endPoint));
                                            model.setRouteViaPoint(object.getString(RoutesSelectConstants.viaPoint));
                                            list.add(model);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    adapter = new AllRoutesAdapter(list, AllRoutes.this);
                                    recyclerView.setAdapter(adapter);
                                } else {
                                   showError();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.d("ERR","222");
                                showError();
                            }
                        } else if (response.has("SearchErrorSelecting")){
                            recyclerView.setVisibility(View.INVISIBLE);
                            searcherror.setVisibility(View.VISIBLE);
                        }else {
                            Log.d("ERR","1");
                            showError();
                        }
                    }
                    @Override
                    public void onError(ANError anError) {
                        loadToast.error();
                        Log.d("ERR",anError.toString());
                        showError();
                    }
                });
    }

    public void showError() {
        Log.d("ERR","in");
        loadToast.error();
        Snackbar.make(rootView, getString(R.string.tryagainlater), Snackbar.LENGTH_INDEFINITE).setAction("Retry", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connection.isInternet()) {
                    getAllRoutesData(areaName);
                }
            }
        });
    }
}
