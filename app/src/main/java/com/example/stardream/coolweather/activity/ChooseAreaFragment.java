package com.example.stardream.coolweather.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.stardream.coolweather.R;
import com.example.stardream.coolweather.db.*;
import com.example.stardream.coolweather.util.HttpUtil;
import com.example.stardream.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY =2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    private Province selectedProvince;
    private City selectedCity;
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        //载入listView
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //设置监听事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE){

                    selectedProvince = provinceList.get(position);
                    Log.d(TAG,"selectedProvince.name="+selectedProvince.getProvinceName()+"  provinceId="+selectedProvince.getId());
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    //记住选中的City
                    selectedCity = cityList.get(position);
                    Log.d(TAG,"selectedCity.name="+selectedCity.getCityName()+"  +"+"provinceId="+selectedCity.getProvinceId());
                    //切换到相应的county界面
                    queryCounties();
                }else if(currentLevel == LEVEL_COUNTY){
                    String weatherId = countyList.get(position).getWeatherId();
                    Log.d(TAG,"countyName="+countyList.get(position).getCountyName()+" CityId="+countyList.get(position).getCityId());
                    Log.d(TAG,"weatherId="+weatherId);
                    Log.d(TAG,"provinceName="+selectedProvince.getProvinceName()+" cityName"+selectedCity.getCityName()+" id="+selectedCity.getId()
                    +" code="+selectedCity.getCityCode());

                    if(getActivity()instanceof MainActivity){
                        Log.d(TAG,"MainActivity");
                        Intent intent = new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){

                        WeatherActivity activity = (WeatherActivity)getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                //若在county切换到City
                if(currentLevel == LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    //若在City切换到province
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }
    private void queryProvinces(){
        //设置标题栏
        titleText.setText("中国");
        //隐藏返回按钮
        backButton.setVisibility(View.GONE);
        //查询所有省份
        provinceList = DataSupport.findAll(Province.class);
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);

            listView.setAdapter(adapter);    //载入listView
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else{
            String address = "http://guolin.tech/api/china";    //连接服务器
            queryFromServer(address,"province");
        }
    }
    private void queryCities(){

        int cityListNum=0;
        //设置标题栏
        titleText.setText(selectedProvince.getProvinceName());
        //设置返回按钮可见
        backButton.setVisibility(View.VISIBLE);
        //在数据库中查询对应的City数据
        //应该取出的是选中省份的city
        cityList = DataSupport.where("provinceId = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        for(City city : cityList){
            if(city.getProvinceId() == selectedProvince.getId()){
                cityListNum++;
            }
        }
        if(cityListNum>0){
            dataList.clear();
            for(City city : cityList){
                if(city.getProvinceId()==selectedProvince.getId())
                        dataList.add(city.getCityName());
            }
        }
        else{
            String address = "http://guolin.tech/api/china/"+selectedProvince.getProvinceCode();
            queryFromServer(address,"city");
        }
        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);

        listView.setAdapter(adapter);//载入listView
        adapter.notifyDataSetChanged();
        listView.setSelection(0);
        currentLevel = LEVEL_CITY;
    }
    /*查询选中的市内的所有县，优先从数据库查，若没有则去服务器查询
    * */
    private static final String TAG="chooseAreaFragment";
    private void queryCounties(){
        int countyListNum=0;
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        //在数据库中查询对应的county数据
        countyList = DataSupport.where("cityId = ?",String.valueOf(selectedCity.getId())).find(County.class);
        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        //载入listView
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        listView.setSelection(0);
        currentLevel = LEVEL_COUNTY;
        Log.d(TAG,"Province="+selectedProvince.getId()+"  city="+selectedCity.getCityCode()+"city.provcince="+selectedCity.getProvinceId());
        for(County county : countyList){
            if(county.getCityId() == selectedCity.getId() ){
                countyListNum++;
            }
        }
        if(countyListNum>0){
            dataList.clear();
            for(County county:countyList){
                if(county.getCityId()==selectedCity.getId())
                     dataList.add(county.getCountyName());
            }
        }else{
            String address = "http://guolin.tech/api/china/"+
                    selectedProvince.getProvinceCode()+"/"+selectedCity.getCityCode();
            Log.d(TAG,"address="+address);
            queryFromServer(address,"county");
        }
    }

    private void queryFromServer(String address,final String type){
        showProgressDialog();
        Log.d(TAG,"发送之前");
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if(type.equals("province")){
                    result = Utility.hanldeProvinceResponse(responseText);
                }else if(type.equals("city")){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if(type.equals("county")){
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if(type.equals("province")){
                                queryProvinces();
                            }else if(type.equals("city")){
                                queryCities();
                            }else if(type.equals("county")){
                                queryCounties();
                            }
                        }
                    });
                }


            }
        });
    }
    //显示进度条框
    private void showProgressDialog(){
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载…");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    //关闭进度框
    private void closeProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }
}
