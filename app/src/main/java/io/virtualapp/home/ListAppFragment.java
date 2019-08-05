package io.virtualapp.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.ui.VFragment;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.adapters.AppChooseAct;
import io.virtualapp.home.adapters.CloneAppListAdapter;
import io.virtualapp.home.adapters.decorations.ItemOffsetDecoration;
import io.virtualapp.home.models.AppInfo;
import io.virtualapp.home.models.AppInfoLite;
import io.virtualapp.widgets.DragSelectRecyclerView;
import jonathanfinerty.once.Once;

/**
 * @author Lody
 */
public class ListAppFragment extends VFragment<ListAppContract.ListAppPresenter> implements ListAppContract.ListAppView {
    private static final String KEY_SELECT_FROM = "key_select_from";
    private DragSelectRecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private Button mInstallButton;
    private CloneAppListAdapter mAdapter;
    List<AppInfo> privList;

    public static ListAppFragment newInstance(File selectFrom) {
        Bundle args = new Bundle();
        if (selectFrom != null)
            args.putString(KEY_SELECT_FROM, selectFrom.getPath());
        ListAppFragment fragment = new ListAppFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private File getSelectFrom() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            String selectFrom = bundle.getString(KEY_SELECT_FROM);
            if (selectFrom != null) {
                return new File(selectFrom);
            }
        }
        return null;
    }

    public void startRemoteThread()
    {
        new ListAppPresenterImpl(getActivity(), this, getSelectFrom()).start();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_app, null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.saveInstanceState(outState);
    }

    public void onSearchAppByName(String szToSearch)
    {
        if(privList==null)return;
        if(privList.size()==0)return;
        List<AppInfo> theListChg;
        try
        {
            theListChg = privList.subList(0, privList.size());
        }
        catch(Throwable e)
        {
            e.printStackTrace();
            return;
        }
        Iterator<AppInfo> theItor = theListChg.iterator();
        while (theItor.hasNext())
        {
            AppInfo theInfo = theItor.next();
            if (!theInfo.name.toString().contains(szToSearch))
                theItor.remove();
        }
        mAdapter.setList(theListChg);
    }

    private SearchView hSearch;
    private SwipeRefreshLayout hLayoutSwipe;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        try
        {
            if (getContext() == null) return;
            if (getActivity() == null) return;
            mRecyclerView = (DragSelectRecyclerView) view.findViewById(R.id.select_app_recycler_view);
            mProgressBar = (ProgressBar) view.findViewById(R.id.select_app_progress_bar);
            mInstallButton = (Button) view.findViewById(R.id.select_app_install_btn);
            FloatingActionButton hButton = view.findViewById(R.id.buttonAddByPath);
            Button hSearchButton = (Button) view.findViewById(R.id.search_app_m);
            if (Once.beenDone("enable_search_app"))
            {
                hSearchButton.setVisibility(View.VISIBLE);
            }
            mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, OrientationHelper.VERTICAL));
            mRecyclerView.addItemDecoration(new ItemOffsetDecoration(VUiKit.dpToPx(getContext(), 2)));
            mAdapter = new CloneAppListAdapter(getActivity());
            mRecyclerView.setAdapter(mAdapter);
            mAdapter.setOnItemClickListener(new CloneAppListAdapter.ItemEventListener()
            {
                @Override
                public void onItemClick(AppInfo info, int position)
                {
                    int count = mAdapter.getSelectedCount();
                    if (!mAdapter.isIndexSelected(position))
                    {
                        if (count >= 9)
                        {
                            Toast.makeText(getContext(), R.string.install_too_much_once_time, Toast.LENGTH_SHORT).show();
                            // return;
                        }
                    }
                    mAdapter.toggleSelected(position);
                }

                @Override
                public boolean isSelectable(int position)
                {
                    return true;
                    //return mAdapter.isIndexSelected(position) || mAdapter.getSelectedCount() < 9;
                }
            });
            mAdapter.setSelectionListener(count ->
            {
                mInstallButton.setEnabled(count > 0);
                try
                {
                    mInstallButton.setText(String.format(Locale.ENGLISH, getResources().getString(R.string.install_d), count));
                } catch (Throwable e)
                {
                    e.printStackTrace();
                }
            });
            mInstallButton.setOnClickListener(v ->
            {
                Integer[] selectedIndices = mAdapter.getSelectedIndices();
                ArrayList<AppInfoLite> dataList = new ArrayList<AppInfoLite>(selectedIndices.length);
                for (int index : selectedIndices)
                {
                    try
                    {
                        AppInfo info = mAdapter.getItem(index);
                        dataList.add(new AppInfoLite(info.packageName, info.path, info.fastOpen));
                    } catch (Throwable e)
                    {
                        e.printStackTrace();
                    }
                }
                Intent data = new Intent();
                data.putParcelableArrayListExtra(VCommends.EXTRA_APP_INFO_LIST, dataList);
                getActivity().setResult(Activity.RESULT_OK, data);
                getActivity().finish();
            });
            hButton.setOnClickListener(v ->
            {
                AppChooseAct.pActParent = this;
                Intent hIntent = new Intent(getActivity(), AppChooseAct.class);
                getActivity().startActivityForResult(hIntent, AppChooseAct.resultCore);
            });
            hSearch = view.findViewById(R.id.search_box_vi);
            hSearchButton.setOnClickListener(v ->
            {
                hSearch.setVisibility(View.VISIBLE);
                Toast.makeText(getActivity(), R.string.click_search_tips, Toast.LENGTH_LONG).show();
            });
            hSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener()
            {
                @Override
                public boolean onQueryTextSubmit(String query)
                {
                    hSearch.setVisibility(View.INVISIBLE);
                    privList = mAdapter.getList();
                    onSearchAppByName(query);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText)
                {
                    return false;
                }
            });
            hLayoutSwipe = view.findViewById(R.id.swipeRefreshInstalled);
            hLayoutSwipe.setOnRefreshListener(() ->
            {
                new ListAppPresenterImpl(getActivity(),
                        ListAppFragment.this, getSelectFrom()).start();
                hLayoutSwipe.setRefreshing(false);
            });
            startRemoteThread();
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void startLoading() {
        mProgressBar.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void loadFinish(List<AppInfo> infoList) {
        mAdapter.setList(infoList);
        mRecyclerView.setDragSelectActive(false, 0);
        mAdapter.setSelected(0, false);
        mProgressBar.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void setPresenter(ListAppContract.ListAppPresenter presenter) {
        this.mPresenter = presenter;
    }

}
