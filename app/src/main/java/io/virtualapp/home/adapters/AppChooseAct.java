package io.virtualapp.home.adapters;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lody.virtual.client.core.RomChecker;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.compat.BuildCompat;
import com.sk.installapp.InstallPkgAct;
import com.zhongcheng.openva.zcutil.BufferUtils;
import com.zyao89.view.zloading.ZLoadingDialog;
import com.zyao89.view.zloading.Z_TYPE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import io.virtualapp.R;
import io.virtualapp.home.ListAppFragment;
import jonathanfinerty.once.Once;

public class AppChooseAct extends AppCompatActivity
{
    static public ListAppFragment pActParent = null;
    static private final Integer requestSAFCode = 404;

    private void setupChooseAct()
    {
        try
        {
            if (/*!Once.beenDone("disable_safe_mode")*/false)
            {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.about)
                        .setMessage(R.string.safe_mode_enforcing)
                        .setCancelable(false)
                        .setPositiveButton(R.string.back, (dialog, which) ->
                                finish())
                        .create().show();
            }
            else if (!Once.beenDone("appchoose_act_tips"))
            {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.about)
                        .setMessage(R.string.appchoose_tips)
                        .setCancelable(false)
                        .setPositiveButton(R.string.accept, (dialog, which) ->
                        {
                            Once.markDone("appchoose_act_tips");
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            startActivityForResult(intent, requestSAFCode);
                        })
                        .create().show();
            } else
            {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, requestSAFCode);
            }
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        try{
            setTheme(R.style.Translucent_NoTitle);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        setContentView(R.layout.activity_app_choose);
        /*
        AlertDialog.Builder hBuilder = new AlertDialog.Builder(this);
        hBuilder.setMessage(R.string.use_sk_installer);
        hBuilder.setTitle(R.string.SK_Settings);
        hBuilder.setOnCancelListener(dialogInterface -> setupChooseAct());
        hBuilder.setNegativeButton(R.string.do_not_use, (dialogInterface, i) ->
        {
            useSKInstaller = false;
            setupChooseAct();
        });
        hBuilder.setPositiveButton(R.string.use, (dialogInterface, i) ->
        {
            useSKInstaller = true;
            setupChooseAct();
        })
                .setCancelable(true)
                .create().show();
         */

        setupChooseAct();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
     */
    public String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                // May lead crash for ADUI.
                if(RomChecker.isMiui())return null;

                final String id = DocumentsContract.getDocumentId(uri);
                Uri contentUri = uri;// = ContentUris.withAppendedId(
                //        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                }
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        String path = "";
        Uri uri = null;
        if(requestCode!=requestSAFCode||resultCode!=RESULT_OK)
        {
            finish();
            return;
        }
        if (data != null)
        {
            try
            {
                uri = data.getData();
            }catch (Throwable e)
            {
                e.printStackTrace();
                finish();
                return;
            }
        }
        else
        {
            finish();
            return;
        }
        if(uri==null)
        {
            finish();
            return;
        }
        /*
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
            try
            {
                String szExStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
                try
                {
                    if (Objects.requireNonNull(uri.getPath()).startsWith(szExStorage))
                    {
                        path = uri.getPath();
                    } else path = getPath(this, uri);
                }
                catch (Throwable e)
                {
                    path = getPath(this, uri);
                }
            }catch(Throwable e)
            {
                e.printStackTrace();
                finish();
                return;
            }
        }
        else
        {
            finish();
            return;
        }
        if(pActParent==null||path==null)
        {
            finish();
            return;
        }
         */

        // 推荐使用安装器安装，选项更多
        final Uri finalUri = uri;
        final ZLoadingDialog zLoadingDialog = new ZLoadingDialog(this);
        zLoadingDialog.setHintTextColor(Color.BLACK);
        zLoadingDialog.setCancelable(false);
        zLoadingDialog.setLoadingBuilder(Z_TYPE.MUSIC_PATH);
        zLoadingDialog.setHintText("Loading");
        zLoadingDialog.show();
        new Thread(()->
        {
            try
            {
                File hFile = BufferUtils.fromUriToBuffer(this,finalUri);
                Intent lpInstaller = new Intent(this, InstallPkgAct.class);
                lpInstaller.putExtra("apk_path",hFile.getAbsolutePath());
                startActivity(lpInstaller);
                setResult(0x0);
            } catch (Throwable e)
            {
                e.printStackTrace();
            }
            runOnUiThread(()->
            {
                zLoadingDialog.dismiss();
                finish();
            });
        }).start();

        /*
        if(pActParent!=null)
            if(pActParent.getActivity()!=null)
            {
                pActParent.getActivity().finish();
                pActParent = null;
            }
         */
        /*
        if (useSKInstaller)
        {
        }
        else
        {
            try
            {
                //if (HomeActivity.hHomeAct != null)
                //    HomeActivity.hHomeAct.InstallAppByPath(path);
                Intent hIntent = new Intent();
                hIntent.putExtra("path",path);
                setResult(resultCode,hIntent);
            }catch (Throwable e)
            {
                e.printStackTrace();
            }
        }
         */
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    public static final int resultCore = 0x2e;
}
