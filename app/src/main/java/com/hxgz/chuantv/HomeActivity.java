package com.hxgz.chuantv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.alibaba.fastjson.JSON;
import com.hxgz.chuantv.dataobject.NavItemDO;
import com.hxgz.chuantv.dataobject.SectionItemDO;
import com.hxgz.chuantv.dataobject.VideoInfoDO;
import com.hxgz.chuantv.dataobject.VideoSectionPageDO;
import com.hxgz.chuantv.extractors.TVExtractor;
import com.hxgz.chuantv.utils.IntentUtil;
import com.hxgz.chuantv.utils.LogUtil;
import com.hxgz.chuantv.utils.NoticeUtil;
import com.hxgz.chuantv.widget.ImageCardView.TestMoviceListPresenter;
import com.open.androidtvwidget.bridge.RecyclerViewBridge;
import com.open.androidtvwidget.leanback.adapter.GeneralAdapter;
import com.open.androidtvwidget.leanback.mode.*;
import com.open.androidtvwidget.leanback.recycle.RecyclerViewTV;
import com.open.androidtvwidget.view.MainUpView;
import com.owen.tab.TvTabLayout;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhoujianwu
 * @date 2020/10/18
 * @description：
 */
public class HomeActivity extends Activity implements RecyclerViewTV.OnItemListener {
    TVExtractor tvExtractor;

    TvTabLayout mTabLayout;
    VideoSectionPageDO videoSectionPageDO;

    MainUpView mMainUpView;
    RecyclerViewTV mRecyclerView;
    RecyclerViewBridge mRecyclerViewBridge;
    List<ListRow> sectionViewList = new ArrayList<>();
    ListRowPresenter mListRowPresenter;

    View oldView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExtractor = App.getTVForSearch();

        View searchAction = findViewById(R.id.searchAction);
        searchAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent detailIntent = new Intent(HomeActivity.this, SearchActivity.class);
                startActivity(detailIntent);
            }
        });

        initTab();

        initSection();

        loadData(null);

    }

    public float getDimension(int id) {
        return getResources().getDimension(id);
    }

    public void initTab() {
        mTabLayout = findViewById(R.id.navTab);
        mTabLayout.addOnTabSelectedListener(new TvTabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TvTabLayout.Tab tab) {
                int position = tab.getPosition();

                if (videoSectionPageDO == null || videoSectionPageDO.getNavItemDOList().size() == 0) {
                    return;
                }

                if (mListRowPresenter != null) {
                    loadData(videoSectionPageDO.getNavItemDOList().get(position).getNavId());
                }
            }

            @Override
            public void onTabUnselected(TvTabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TvTabLayout.Tab tab) {
                LogUtil.e(String.valueOf("DDD"));

            }
        });

        mTabLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TvTabLayout tvTabLayout = (TvTabLayout) v;
                NavItemDO itemDO = videoSectionPageDO.getNavItemDOList().get(tvTabLayout.getSelectedTabPosition());

                Intent detailIntent = new Intent(HomeActivity.this, PickerActivity.class);
                IntentUtil.putData(detailIntent, "refer", itemDO.getNavId());
                startActivity(detailIntent);
            }
        });
    }

    public void initSection() {
        mRecyclerView = (RecyclerViewTV) findViewById(R.id.recyclerView);
        mMainUpView = (MainUpView) findViewById(R.id.mainUpView);
        mMainUpView.setEffectBridge(new RecyclerViewBridge());
        // 注意这里，需要使用 RecyclerViewBridge 的移动边框 Bridge.
        mRecyclerViewBridge = (RecyclerViewBridge) mMainUpView.getEffectBridge();
        //mRecyclerViewBridge.setUpRectResource(R.drawable.video_cover_cursor);
        float density = getResources().getDisplayMetrics().density;
        RectF receF = new RectF(getDimension(R.dimen.h_45) * density, getDimension(R.dimen.h_40) * density,
                getDimension(R.dimen.h_45) * density, getDimension(R.dimen.h_40) * density);
        mRecyclerViewBridge.setDrawUpRectPadding(receF);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setSelectedItemAtCentered(true); // 设置item在中间移动.

        mListRowPresenter = new ListRowPresenter(
                sectionViewList, new ItemHeaderPresenter(), new ItemListPresenter());
        GeneralAdapter generalAdapter = new GeneralAdapter(mListRowPresenter);
        mRecyclerView.setAdapter(generalAdapter);

        mRecyclerView.setOnItemListener(this);
    }

    private void loadData(final String navId) {
        new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                try {
                    videoSectionPageDO = tvExtractor.previewNav(navId);
                } catch (Exception e) {
                    NoticeUtil.show(HomeActivity.this, "发生异常,请重试");
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mTabLayout.getTabCount() == 0) {
                            for (NavItemDO itemDO : videoSectionPageDO.getNavItemDOList()) {
                                TvTabLayout.Tab tab = mTabLayout.newTab().setText(itemDO.getTitle());
                                mTabLayout.addTab(tab);
                            }
                        }
                        //mTabLayout.selectTab(0);

                        // 榜单数据
                        sectionViewList.clear();
                        for (SectionItemDO sectionItemDO : videoSectionPageDO.getSectionItemDOList()) {
                            ListRow sectionRow = new ListRow(sectionItemDO.getTitle());
                            TestMoviceListPresenter presenter = new TestMoviceListPresenter();
                            presenter.setOnItemClickListener(new RecyclerViewTV.OnItemClickListener() {
                                @Override
                                public void onItemClick(RecyclerViewTV parent, View itemView, int position) {
                                    GeneralAdapter adapter = (GeneralAdapter) parent.getAdapter();
                                    VideoInfoDO videoInfoDO = (VideoInfoDO) ((DefualtListPresenter) adapter.getPresenter()).getItem(position);
                                    Intent detailIntent = new Intent(HomeActivity.this, VideoDetailActivity.class);
                                    IntentUtil.putData(detailIntent, "videoInfoDO", videoInfoDO);
                                    startActivity(detailIntent);
                                }
                            });
                            sectionRow.setOpenPresenter(presenter); // 设置列的item样式.

                            for (VideoInfoDO videoInfoDO : sectionItemDO.getVideoInfoDOList()) {
                                sectionRow.add(videoInfoDO);
                            }
                            sectionViewList.add(sectionRow);
                        }

                        mListRowPresenter.setItems(sectionViewList);
                        mRecyclerView.getAdapter().notifyDataSetChanged();
                        //mListRowPresenter.setDefaultPos(0, 0); // 设置默认选中.

                    }
                });
            }
        }).start();
    }

    /**
     * 排除 Leanback demo的RecyclerView.
     */
    private boolean isListRowPresenter() {
        GeneralAdapter generalAdapter = (GeneralAdapter) mRecyclerView.getAdapter();
        OpenPresenter openPresenter = generalAdapter.getPresenter();
        return (openPresenter instanceof ListRowPresenter);
    }

    @Override
    public void onItemPreSelected(RecyclerViewTV parent, View itemView, int position) {
        if (!isListRowPresenter()) {
            mRecyclerViewBridge.setUnFocusView(oldView);
        }
    }

    @Override
    public void onItemSelected(RecyclerViewTV parent, View itemView, int position) {
        if (!isListRowPresenter()) {
            mRecyclerViewBridge.setFocusView(itemView, 1.2f);
            oldView = itemView;
        }
    }

    @Override
    public void onReviseFocusFollow(RecyclerViewTV parent, View itemView, int position) {
        if (position == 0) {
            mRecyclerView.smoothScrollToPosition(0);
        }

        if (!isListRowPresenter()) {
            mRecyclerViewBridge.setFocusView(itemView, 1.2f);
            oldView = itemView;
        }
    }

}

    