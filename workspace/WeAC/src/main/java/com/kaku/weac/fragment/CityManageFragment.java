package com.kaku.weac.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.kaku.weac.R;
import com.kaku.weac.activities.AddCityActivity;
import com.kaku.weac.adapter.CityManageAdapter;
import com.kaku.weac.bean.CityManage;
import com.kaku.weac.bean.WeatherDaysForecast;
import com.kaku.weac.bean.WeatherInfo;
import com.kaku.weac.common.WeacConstants;
import com.kaku.weac.db.WeatherDBOperate;
import com.kaku.weac.Listener.DBObserverListener;
import com.kaku.weac.Listener.HttpCallbackListener;
import com.kaku.weac.util.HttpUtil;
import com.kaku.weac.util.MyUtil;
import com.kaku.weac.util.ToastUtil;
import com.kaku.weac.util.WeatherUtil;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 城市管理fragment
 *
 * @author 咖枯
 * @version 1.0 2015/10/22
 */
public class CityManageFragment extends Fragment implements View.OnClickListener {

    /**
     * Log tag ：CityManageFragment
     */
    private static final String LOG_TAG = "CityManageFragment";

    /**
     * 城市管理的requestCode
     */
    private static final int REQUEST_CITY_MANAGE = 1;

    /**
     * 城市管理列表
     */
    private List<CityManage> mCityManageList;

    /**
     * 城市管理adapter
     */
    private CityManageAdapter mCityManageAdapter;

    /**
     * 天气信息
     */
    private WeatherInfo mWeatherInfo;

    /**
     * 进度对话框
     */
    private ProgressDialog mProgressDialog;

    /**
     * 天气代码
     */
    private String mWeatherCode;

    /**
     * 城市管理GridView
     */
    private GridView mGridView;

    /**
     * 操作栏编辑按钮
     */
    private ImageView mEditAction;

    /**
     * 操作栏编辑完成按钮
     */
    private ImageView mAcceptAction;

    /**
     * 监听城市点击事件Listener
     */
    private AdapterView.OnItemClickListener mOnItemClickListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCityManageList = new ArrayList<>();
        mCityManageList = WeatherDBOperate.getInstance().loadCityManages();
        // 添加城市按钮用
        CityManage cityManage1 = new CityManage();
        mCityManageList.add(cityManage1);
        mCityManageAdapter = new CityManageAdapter(getActivity(), mCityManageList);
        // 城市列表为空的更新回调
        mCityManageAdapter.setDBObserverListener(new DBObserverListener() {
            @Override
            public void onDBDataChanged() {
                // 允许列表item点击
                mGridView.setOnItemClickListener(mOnItemClickListener);
                // 显示编辑按钮
                mEditAction.setVisibility(View.VISIBLE);
                // 隐藏完成按钮
                mAcceptAction.setVisibility(View.GONE);
            }
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fm_city_manage, container, false);
        LinearLayout backGround = (LinearLayout) view.findViewById(R.id.city_manage_background);
        // 设置页面背景
        MyUtil.setBackgroundBlur(backGround, getActivity());

        mOnItemClickListener = new OnItemClickListenerImpl();
        mGridView = (GridView) view.findViewById(R.id.gv_city_manage);
        mGridView.setAdapter(mCityManageAdapter);
        mGridView.setOnItemClickListener(mOnItemClickListener);
        mGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position != (mCityManageList.size() - 1)) {
                    // 显示删除，完成按钮，隐藏修改按钮
                    displayDeleteAccept();
                }
                return true;
            }
        });

        // 返回按钮
        ImageView returnBtn = (ImageView) view.findViewById(R.id.action_return);
        returnBtn.setOnClickListener(this);

        // 编辑闹钟
        mEditAction = (ImageView) view.findViewById(R.id.action_edit);
        mEditAction.setOnClickListener(this);

        // 完成按钮
        mAcceptAction = (ImageView) view.findViewById(R.id.action_accept);
        mAcceptAction.setOnClickListener(this);

        return view;
    }

    class OnItemClickListenerImpl implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // 当为列表最后一项（添加城市）
            if (position == (mCityManageList.size() - 1)) {
                Intent intent = new Intent(getActivity(), AddCityActivity.class);
                startActivityForResult(intent, REQUEST_CITY_MANAGE);
            } else {
                SharedPreferences share = getActivity().getSharedPreferences(
                        WeacConstants.EXTRA_WEAC_SHARE, Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = share.edit();
                CityManage cityManage = mCityManageAdapter.getItem(position);
                // 保存默认的天气代码
                editor.putString(WeacConstants.WEATHER_CODE, cityManage.getWeatherCode());
                // 保存默认的城市名
                editor.putString(WeacConstants.DEFAULT_CITY_NAME, cityManage.getCityName());
                editor.apply();

                Intent intent = getActivity().getIntent();
                getActivity().setResult(Activity.RESULT_OK, intent);
                getActivity().finish();
            }
        }
    }

    /**
     * 显示删除，完成按钮，隐藏修改按钮
     */
    private void displayDeleteAccept() {
        // 禁止gridView点击事件
        mGridView.setOnItemClickListener(null);
        mCityManageAdapter.setCityDeleteButton(true);
        mCityManageAdapter.notifyDataSetChanged();
        mEditAction.setVisibility(View.GONE);
        mAcceptAction.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏删除，完成按钮,显示修改按钮
     */
    private void hideDeleteAccept() {
        mGridView.setOnItemClickListener(mOnItemClickListener);
        mCityManageAdapter.setCityDeleteButton(false);
        mCityManageAdapter.notifyDataSetChanged();
        mAcceptAction.setVisibility(View.GONE);
        mEditAction.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // 返回按钮
            case R.id.action_return:
                getActivity().finish();
                break;
            // 编辑按钮
            case R.id.action_edit:
                // 当列表内容为空时禁止响应编辑事件
                if (mGridView.getChildCount() == 1) {
                    return;
                }
                // 显示删除，完成按钮，隐藏修改按钮
                displayDeleteAccept();
                break;
            // 完成按钮
            case R.id.action_accept:
                // 隐藏删除，完成按钮,显示修改按钮
                hideDeleteAccept();
                break;
        }
    }

    /**
     * 根据传入的代号和类型从服务器上查询天气代号、天气数据
     *
     * @param address 查询地址
     * @param type    查询类型
     */
    private void queryFormServer(String address, final String type) {
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                switch (type) {
                    // 天气代号
                    case WeacConstants.WEATHER_CODE:
//                        try {
                        if (!response.contains("error")) {
                            mWeatherInfo = WeatherUtil.handleWeatherResponse(
                                    new ByteArrayInputStream(response.getBytes()));
                            // 保存天气信息
                            WeatherUtil.saveWeatherInfo(mWeatherInfo, getActivity());
                            // 添加城市列表
                            getActivity().runOnUiThread(new SetCityInfoRunnable());
                        } else {
                            runOnUi(getString(R.string.no_city_info));
                        }
//                        } catch (Exception e) {
//                            LogUtil.e(LOG_TAG, e.toString());
//                        }
                        break;
                    // 县代号
                    case WeacConstants.COUNTRY_CODE:
                        String[] array = response.split("\\|");
                        if (array.length == 2) {
                            mWeatherCode = array[1];
                            // 查询天气代号
                            queryFormServer(getString(R.string.address_weather, mWeatherCode),
                                    WeacConstants.WEATHER_CODE);
                        } else {
                            runOnUi(getString(R.string.no_city_info));
                        }
                        break;
                }
            }

            @Override
            public void onError(Exception e) {
//                try {
                runOnUi(getString(R.string.Internet_fail));
//                } catch (Exception e1) {
//                    LogUtil.e(LOG_TAG, e1.toString());
//                }
            }
        });
    }

    /**
     * 执行UI方法
     *
     * @param info 显示的错误信息
     */
    private void runOnUi(final String info) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closeProgressDialog();
                ToastUtil.showLongToast(getActivity(), info);
            }
        });
    }

    /**
     * 设置添加城市列表信息
     */
    public class SetCityInfoRunnable implements Runnable {

        @Override
        public void run() {
            closeProgressDialog();

            CityManage cityManage = new CityManage();
            cityManage.setWeatherCode(mWeatherCode);
            cityManage.setCityName(mWeatherInfo.getCity());

            if (mWeatherInfo.getWeatherDaysForecast().size() == 6) {
                WeatherDaysForecast weather;

                String time[] = mWeatherInfo.getUpdateTime().split(":");
                int hour = Integer.parseInt(time[0]);
                int minute = Integer.parseInt(time[1]);

                //更新时间从23：45开始到05：20以前的数据，后移一天填充
                if ((hour == 23 && minute >= 45) || (hour < 5) ||
                        ((hour == 5) && (minute < 20))) {
                    weather = mWeatherInfo.getWeatherDaysForecast().get(2);
                } else {
                    weather = mWeatherInfo.getWeatherDaysForecast().get(1);
                }

                cityManage.setTempHigh(weather.getHigh().substring(3));
                cityManage.setTempLow(weather.getLow().substring(3));

                // 天气类型图片id
                int weatherId;
                // 设置今天天气信息
                // 当前为凌晨
                if (hour >= 0 && hour < 6) {
                    weatherId = WeaFragment.getWeatherTypeImageID(weather.getTypeDay(), false);
                    // 当前为白天时
                } else if (hour >= 6 && hour < 18) {
                    weatherId = WeaFragment.getWeatherTypeImageID(weather.getTypeDay(), true);
                    // 当前为夜间
                } else {
                    weatherId = WeaFragment.getWeatherTypeImageID(weather.getTypeNight(), false);
                }
                cityManage.setImageId(weatherId);

                // 白天和夜间类型相同
                if (weather.getTypeDay().equals(weather.getTypeNight())) {
                    cityManage.setWeatherType(weather.getTypeDay());
                } else {
                    cityManage.setWeatherType(String.format(getString(R.string.turn),
                            weather.getTypeDay(), weather.getTypeNight()));
                }
            } else {
                cityManage.setTempHigh(getString(R.string.dash));
                cityManage.setTempLow(getString(R.string.dash));
                cityManage.setImageId(R.drawable.ic_weather_no);
                cityManage.setWeatherType(getString(R.string.no));
            }

            // 插在最后一项（添加）之前
            mCityManageAdapter.notifyDataSetChanged();
            mCityManageList.add(mCityManageList.size() - 1, cityManage);
            // 存储城市管理信息
            WeatherDBOperate.getInstance().saveCityManage(cityManage);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CITY_MANAGE) {
            String countryCode = data.getStringExtra(WeacConstants.COUNTRY_CODE);
            showProgressDialog();
            queryFormServer(getString(R.string.address_city, countryCode),
                    WeacConstants.COUNTRY_CODE);
        }
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage(getString(R.string.now_loading));
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }
}